package it.polimi.nsds.spark.labs.friends;

import it.polimi.nsds.spark.tutorial.common.Consts;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;

import java.util.ArrayList;
import java.util.List;

import static org.apache.spark.sql.functions.col;

public class FriendsComputation {
    private static final boolean useCache = true;

    public static void main(String[] args) {
        final String master = args.length > 0 ? args[0] : "local[4]";
        final String filePath = args.length > 1 ? args[1] : Consts.FILE_PATH_DEFAULT;
        final String appName = args.length > 2 ? args[2] : "friends";

        final SparkSession spark = SparkSession
                .builder()
                .master(master)
                .appName(appName)
                .getOrCreate();
        spark.sparkContext().setLogLevel("ERROR");

        final List<StructField> mySchemaFields = new ArrayList<>();
        mySchemaFields.add(DataTypes.createStructField("user", DataTypes.StringType, true));
        mySchemaFields.add(DataTypes.createStructField("friend", DataTypes.StringType, true));
        final StructType mySchema = DataTypes.createStructType(mySchemaFields);

        Dataset<Row> friends = spark
                .read()
                .option("header", "false")
                .option("delimiter", ",")
                .schema(mySchema)
                .csv(filePath + "labs/friends/friends.csv");


        friends.show();

        // Let's close it!
        if(useCache) {
            friends.cache();
        }

        Dataset<Row> closure;
        while (true) {
            // compute candidate new edges: (u -> w) from (u -> v) and (v -> w)
            closure = transitiveClosureStep(friends);

            // filter out nulls (no match) and already-existing edges
            Dataset<Row> nonNull = closure.filter(col("friend").isNotNull());

            // only keep edges that are not already present in `friends`
            Dataset<Row> newEdges = nonNull.except(friends);

            // stop when no new edges were generated
            if (newEdges.limit(1).count() == 0) {
                break;
            }

            // add the new edges to the closure set
            friends = friends.union(newEdges).distinct();
            if (useCache) {
                friends.cache();
            }
        }

        friends.show();

    }

    public static Dataset<Row> transitiveClosureStep(Dataset<Row> friends) {
        Dataset<Row> joined = friends.alias("friends").join(
                friends.alias("closure"),
                col("friends.friend").equalTo(col("closure.user")),
                "left_outer"
        );
        joined.show();
        return joined.select(col("friends.user").alias("user"),col( "closure.friend").alias(("friend")));
    }
}
