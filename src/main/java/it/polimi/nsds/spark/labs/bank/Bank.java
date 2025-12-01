package it.polimi.nsds.spark.labs.bank;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.apache.spark.sql.functions.*;

/**
 * Bank example
 *
 * Input: csv files with list of deposits and withdrawals, having the following
 * schema ("person: String, account: String, amount: Int)
 *
 * Queries
 * Q1. Print the total amount of withdrawals for each person
 * Q2. Print the person with the maximum total amount of withdrawals
 * Q3. Print all the accounts with a negative balance
 * Q4. Print all accounts in descending order of balance
 *
 * The code exemplifies the use of SQL primitives.  By setting the useCache variable,
 * one can see the differences when enabling/disabling cache.
 */
public class Bank {
    private static final boolean useCache = true;

    public static void main(String[] args) throws IOException {
        final String master = args.length > 0 ? args[0] : "local[4]";
        final String filePath = args.length > 1 ? args[1] : "./";
        final String appName = useCache ? "BankWithCache" : "BankNoCache";


        ///  Spark SQL
        ///  Create session
        final SparkSession spark = SparkSession
                .builder()
                .master(master)
                .appName(appName)
                .getOrCreate();
        spark.sparkContext().setLogLevel("ERROR");

        /// Create schema
        final List<StructField> mySchemaFields = new ArrayList<>();
        mySchemaFields.add(DataTypes.createStructField("person", DataTypes.StringType, true));
        mySchemaFields.add(DataTypes.createStructField("account", DataTypes.StringType, true));
        mySchemaFields.add(DataTypes.createStructField("amount", DataTypes.IntegerType, true));
        final StructType mySchema = DataTypes.createStructType(mySchemaFields);

        /// Read files
        final Dataset<Row> deposits = spark
                .read()
                .option("header", "false")
                .option("delimiter", ",")
                .schema(mySchema)
                .csv(filePath + "files/bank/deposits.csv");

        final Dataset<Row> withdrawals = spark
                .read()
                .option("header", "false")
                .option("delimiter", ",")
                .schema(mySchema)
                .csv(filePath + "files/bank/withdrawals.csv");

        // Used in two different queries
        if (useCache) {
            withdrawals.cache();
            deposits.cache();
        }

        // Q1. Total amount of withdrawals for each person
        System.out.println("Total amount of withdrawals for each person");
        Dataset<Row> sumWithdrawals = withdrawals.groupBy("person").sum("amount").select("person", "sum(amount)");
        sumWithdrawals.show();

        if(useCache){
            sumWithdrawals.cache();
        }

        // Q2. Person with the maximum total amount of withdrawals
        System.out.println("Person with the maximum total amount of withdrawals");

        final long maxTotal = sumWithdrawals
                .agg(max("sum(amount)"))
                .first()
                .getLong(0);

        final Dataset<Row> maxWithdrawals = sumWithdrawals.filter(col("sum(amount)").equalTo(maxTotal));
        maxWithdrawals.show();

        // Q3 Accounts with negative balance
        System.out.println("Accounts with negative balance");
        Dataset<Row> accountWithdrawals = withdrawals.groupBy("person","account").agg(sum("amount")).select( "person", "account", "sum(amount)").withColumnRenamed("sum(amount)", "withdrawals");
        Dataset<Row> accountDeposits = deposits.groupBy("account").agg(sum("amount")).select("account", "sum(amount)").withColumnRenamed("sum(amount)", "deposits");

        //either we want null deposits and positive withdrawals or withdrawals greater than deposits
        Dataset<Row> accountWithNegativeBalances = accountWithdrawals.join(
                accountDeposits, accountDeposits.col("account").equalTo(accountWithdrawals.col("account"))
        ).filter(accountDeposits.col("deposits").isNull().and(accountWithdrawals.col("withdrawals").gt(0)).or
                (accountWithdrawals.col("withdrawals").gt(accountDeposits.col("deposits")))
        ).select(accountWithdrawals.col("person"), accountWithdrawals.col("account"));

        accountWithNegativeBalances.show();

        // Q4 Accounts in descending order of balance
        System.out.println("Accounts in descending order of balance");
        Dataset<Row> balances = accountWithdrawals.join(
                accountDeposits, accountDeposits.col("account").equalTo(accountWithdrawals.col("account"))
        ).select(accountWithdrawals.col("person"), accountWithdrawals.col("account"),  expr("deposits - withdrawals AS balance")).orderBy(desc("balance"));
        balances.show();

        spark.close();
    }
}