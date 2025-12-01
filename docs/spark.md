# Big Data Processing with Apache Spark
### 1. Fundamentals and Core Concepts

#### 1.1 Apache Spark Overview
**Apache Spark** is a widely adopted, **unified analytical engine** designed for large-scale data processing. It facilitates extracting useful knowledge from massive volumes of data, such as studying user behaviors for customized advertisements.

Spark and similar technologies emerged in the early 2000s, pioneered by the Google **MapReduce framework**. These platforms are designed for computation on **general-purpose cluster computing infrastructures** (not dedicated supercomputers).

A primary design goal is offering a **simple programming API** and **high-level declarative programming models** (similar to SQL) that abstract away distributed computing complexities like data distribution, scheduling, synchronization, and fault tolerance.

#### 1.2 The Data Flow Programming Model
The underlying methodology simplifying distributed data processing is the **data flow programming model**.

In this model, computation is organized as a **graph** where edges represent data, and vertices represent **operators** that transform input data.

Key properties of operators:
1.  **Independence and Statelessness:** Operators do not share memory or state. Their output depends solely on their input, enabling deterministic recomputation.
2.  **Parallelism:** The model enables two types of parallelism:
    *   **Task Parallelism:** Different operators (A, B, C) execute in parallel.
    *   **Data Parallelism:** Multiple instances of the same operator (A1, A2, A3) perform the same operation on different portions of the input data.

#### 1.3 MapReduce and Spark Expressiveness
The classic data flow example is the **word count**. It splits computation into two phases: **Map** (calculating partial counts per document) and **Reduce** (aggregating partial results by word).

The original MapReduce framework was restricted to just these two stages. Spark, however, is **more expressive**:
*   It supports basically **any kind of acyclic data flow graph** (not limited to two stages).
*   It supports iterative computation.
*   It is more efficient, for example, by being able to **cache intermediate data in main memory**.

***

### 2. The Resilient Distributed Dataset (RDD) API

The fundamental abstraction provided by Spark is the **Resilient Distributed Dataset (RDD)**.

An RDD is a collection of elements with three key properties:
1.  **Partitioned:** Elements are partitioned across the cluster nodes.
2.  **Parallel Processing:** Can be processed in parallel across workers.
3.  **Fault Tolerant:** RDDs can be reconstructed or recomputed in case of failure.

#### 2.1 Spark Context and Initialization

A Spark program accesses the Spark cluster by instantiating a **Spark Context** object, which holds relevant parameters like the application name and the address of the master.

Spark supports multiple languages, including **Scala, Java, and Python**. The examples provided below are based on the Java API.

**Code Example: Context Initialization (Java)**

```java
// Configure application name and master address (e.g., local for 4 cores)
SparkConf conf = new SparkConf()
    .setAppName("MySparkApp")
    .setMaster("local");

JavaSparkContext sc = new JavaSparkContext(conf);
// ... Use the context ...
sc.close(); // Close the context after use
```

#### 2.2 RDD Creation

RDDs can be created from various sources, including existing collections in the driver program, local or distributed file systems (like HDFS), databases, or Kafka topics.

**Code Example: RDD Creation (Java)**

```java
// 1. RDD from a local collection (List)
List<String> data = Arrays.asList("A", "B", "C", "D");
JavaRDD<String> dataRDD = sc.parallelize(data);

// 2. RDD from a text file
JavaRDD<String> linesRDD = sc.textFile("filename.txt");
```

#### 2.3 RDD Operations: Transformations and Actions

RDDs support two types of operations:

| Operation Type | Description | Execution Property |
| :--- | :--- | :--- |
| **Transformations** | Create a new RDD from an existing one. | **Lazy:** They do not compute anything when invoked. |
| **Actions** | Return a value to the driver program, triggering the computation. | **Eager:** The driver blocks, waiting for the results, thereby triggering all required prior transformations. |

If an RDD is used by multiple actions, it is recomputed multiple times by default. Programmers can use **caching** (`persist` or `cache`) to save the RDD in memory or on disk for faster reuse.

**Code Example: Transformation (`map`) and Action (`reduce`) (Java)**

This example computes the sum of the length of all lines in a file named `data.txt`:

```java
// 1. Create RDD from file (lines contains strings)
JavaRDD<String> lines = sc.textFile("data.txt");

// 2. Transformation: Convert each line (string) to its length (integer)
// map is a transformation, no computation starts here.
JavaRDD<Integer> linesLen = lines.map(s -> s.length());

// 3. Action: Aggregates all values (line lengths) into a single result (the sum)
// reduce is an action, computation starts now.
int totLen = linesLen.reduce((a, b) -> a + b); 

// The driver program receives the result in totLen.
```

#### 2.4 Common RDD Transformations

**Key Transformations:**

| Transformation | Description | Example |
| :--- | :--- | :--- |
| `map(func)` | Apply a function to each element, return new RDD | `rdd.map(s -> s.length())` |
| `flatMap(func)` | Apply a function that returns a sequence, flatten results | `lines.flatMap(line -> Arrays.asList(line.split(" ")).iterator())` |
| `filter(func)` | Select elements where function returns true | `rdd.filter(x -> x > 10)` |
| `mapToPair(func)` | Transform to key-value pairs (JavaPairRDD) | `words.mapToPair(s -> new Tuple2<>(s, 1))` |
| `reduceByKey(func)` | Aggregate values with same key | `pairs.reduceByKey((a, b) -> a + b)` |
| `groupBy(func)` | Group elements by key function | `rdd.groupBy(x -> x % 2)` |
| `distinct()` | Remove duplicate elements | `rdd.distinct()` |
| `union(otherRDD)` | Combine two RDDs | `rdd1.union(rdd2)` |

**Code Example: Complete Word Count with RDD (Java)**

```java
final JavaRDD<String> lines = sc.textFile("in.txt");

// Split lines into words using flatMap
final JavaRDD<String> words = lines.flatMap(line -> 
    Arrays.asList(line.split(" ")).iterator()
);

// Create key-value pairs: (word, 1)
final JavaPairRDD<String, Integer> pairs = words.mapToPair(s -> 
    new Tuple2<>(s, 1)
);

// Aggregate counts by word
final JavaPairRDD<String, Integer> counts = pairs.reduceByKey((a, b) -> 
    a + b
);

// Collect and print results
System.out.println(counts.collect());
```

**Code Example: More Complex RDD Operations (Java)**

```java
// Q1. Count words starting with each character
List<Tuple2<String, Integer>> charCounts = lines
    .flatMap(line -> Arrays.asList(line.split(" ")).iterator())
    .map(String::toLowerCase)
    .mapToPair(s -> new Tuple2<>(s.substring(0, 1), 1))
    .reduceByKey((a, b) -> a + b)
    .collect();

// Q2. Count lines starting with each character
List<Tuple2<String, Integer>> lineCounts = lines
    .map(String::toLowerCase)
    .mapToPair(s -> new Tuple2<>(s.substring(0, 1), 1))
    .reduceByKey((a, b) -> a + b)
    .collect();

// Q3. Compute average line length
Tuple2<Integer, Integer> avgData = lines
    .mapToPair(s -> new Tuple2<>(s.length(), 1))
    .reduce((a, b) -> new Tuple2<>(a._1 + b._1, a._2 + b._2));
double avgLength = ((float) avgData._1) / avgData._2;
```

#### 2.5 Caching and Persistence

When an RDD is used in multiple actions or in iterative computations, it gets recomputed each time. **Caching** stores the RDD in memory (or disk) for faster reuse.

**Code Example: Iterative Computation with Caching (Java)**

```java
// Start with investment data: (amount_owned, interest_rate)
JavaRDD<Tuple2<Double, Double>> investments = textFile.map(w -> {
    String[] values = w.split(" ");
    double amountOwned = Double.parseDouble(values[0]);
    double investmentRate = Double.parseDouble(values[1]);
    return new Tuple2<>(amountOwned, investmentRate);
});

double threshold = 100.0;
double sum = sumAmount(investments);

while (sum < threshold) {
    // Update investments
    investments = investments.map(i -> 
        new Tuple2<>(i._1 * (1 + i._2), i._2)
    );
    
    // Cache for next iteration (important for performance!)
    investments.cache();
    
    sum = sumAmount(investments);
}

// Helper method
private static double sumAmount(JavaRDD<Tuple2<Double, Double>> investments) {
    return investments.mapToDouble(a -> a._1).sum();
}
```

**Note:** Without caching, the entire lineage would be recomputed in each iteration!

***

### 3. Spark Execution and Architecture

A Spark deployment consists of a **Master node** and multiple **Worker nodes**. The Master accepts data processing jobs written inside **Driver Programs**.

#### 3.1 Jobs, DAGs, Stages, and Tasks

When the driver program encounters an **Action**, Spark creates a new data processing **Job**.

1.  **DAG Creation:** The job is represented as a **Directed Acyclic Graph (DAG)** of operators. Intermediate nodes are transformations, and the final node is the action.
2.  **Stage Isolation:** Spark splits the DAG into **Stages**. A stage is a sequence of transformations *without data shuffling* (reorganizing data partitioning) in between. Data shuffling (repartitioning) requires starting a new stage.
3.  **Task Creation:** Each stage is split into multiple **Tasks**, one for each data partition. Tasks are the unit of scheduling, enabling data parallelism.
4.  **Scheduling:** The Master schedules tasks onto available workers, exploiting **data locality** (placing operators close to their input data).

#### 3.2 Fault Tolerance: Lineage
Spark’s fault tolerance mechanism is based on **lineage**. If an RDD is lost due to failure, Spark can **recompute** it by recursively tracing its input RDDs until an available source (like the original data set) is found. Importantly, since RDDs are partitioned, Spark only needs to recompute the **lost partitions**, not the entire dataset.

***

### 4. Spark SQL and Structured Data Processing (Batch)

**Spark SQL** is a module designed for **structured data processing**. Structured data means the data sets have a known **schema** (number, type, and optional names of fields).

#### 4.1 Spark SQL APIs

The schema enables high-level declarative APIs similar to SQL and offers optimization opportunities because the engine knows the data structure.

Spark SQL offers three primary API approaches:

1.  **SQL API:** Text-based standard SQL queries.
    *   *Pro:* Highly optimizable.
    *   *Con:* Limited flexibility; syntax errors are detected only at runtime.
2.  **DataFrame API:** Equivalent to SQL, but embedded as a library in the host language. **DataFrames** are implemented as a `Dataset<Row>` and represent relational tables with fixed, known schemas.
    *   *Pro:* Highly optimizable; syntax errors are detected at compile time.
3.  **Dataset API:** Provides abstraction similar to RDDs, allowing for user-defined functions (UDFs).
    *   *Pro:* More flexibility with UDFs.
    *   *Con:* Reduced opportunities for engine optimization.

To use Spark SQL functionality, developers instantiate a **Spark Session** instead of a Spark Context.

**Code Example: Spark Session Initialization (Java)**

```java
final SparkSession spark = SparkSession.builder()
    .master("local[*]")
    .appName("MySparkSQLApp")
    .getOrCreate();
spark.sparkContext().setLogLevel("ERROR");

// Use the session...
spark.close();
```

**Code Example: DataFrame Operations (Java)**

This example filters rows where 'age' is greater than 18 and selects 'name' and an increased 'salary':

```java
// Assuming 'df' is an initialized DataFrame
df.filter(col("age").gt(18)) 
  .select("name", col("salary").plus(10))
  .show(); // Show displays results in a tabular way
```

**Code Example: Programmatic Schema Definition (Java)**

This defines a schema with a non-nullable string field "name" and a nullable integer field "age":

```java
List<StructField> fields = new ArrayList<>();
// Field 1: Name (String, NOT NULL)
fields.add(DataTypes.createStructField("name", DataTypes.StringType, false));
// Field 2: Age (Integer, NULLABLE)
fields.add(DataTypes.createStructField("age", DataTypes.IntegerType, true));

StructType schema = DataTypes.createStructType(fields);
// This schema can then be used to read CSV or JSON files.
```

#### 4.2 Reading Data with Spark SQL

**Code Example: Reading CSV with Schema (Java)**

```java
// Define schema
List<StructField> mySchemaFields = new ArrayList<>();
mySchemaFields.add(DataTypes.createStructField("person", DataTypes.StringType, true));
mySchemaFields.add(DataTypes.createStructField("account", DataTypes.StringType, true));
mySchemaFields.add(DataTypes.createStructField("amount", DataTypes.IntegerType, true));
StructType mySchema = DataTypes.createStructType(mySchemaFields);

// Read CSV file with schema
Dataset<Row> data = spark.read()
    .option("header", "false")
    .option("delimiter", ",")
    .schema(mySchema)
    .csv("deposits.csv");
```

#### 4.3 Common DataFrame Operations

**Key DataFrame Operations:**

| Operation | Description | Example |
| :--- | :--- | :--- |
| `select()` | Select specific columns | `df.select("name", "age")` |
| `filter()` / `where()` | Filter rows by condition | `df.filter(col("age").gt(18))` |
| `groupBy()` | Group rows by column(s) | `df.groupBy("person")` |
| `agg()` | Aggregate with functions | `df.agg(max("amount"))` |
| `sum()`, `count()`, `avg()` | Aggregate functions | `df.groupBy("person").sum("amount")` |
| `join()` | Join two DataFrames | `df1.join(df2, condition, "inner")` |
| `union()` | Combine two DataFrames | `df1.union(df2)` |
| `distinct()` | Remove duplicate rows | `df.distinct()` |
| `orderBy()` / `sort()` | Sort rows | `df.orderBy(desc("balance"))` |
| `withColumn()` | Add or replace column | `df.withColumn("new", expr("col1 + col2"))` |
| `withColumnRenamed()` | Rename column | `df.withColumnRenamed("old", "new")` |
| `drop()` | Remove column | `df.drop("column_name")` |
| `show()` | Display results | `df.show()` |

**Code Example: Complete Bank Analysis (Java)**

```java
// Read data
Dataset<Row> withdrawals = spark.read()
    .option("header", "false")
    .schema(mySchema)
    .csv("withdrawals.csv");

Dataset<Row> deposits = spark.read()
    .option("header", "false")
    .schema(mySchema)
    .csv("deposits.csv");

// Cache if used multiple times
withdrawals.cache();

// Q1. Total withdrawals per person
Dataset<Row> sumWithdrawals = withdrawals
    .groupBy("person")
    .sum("amount")
    .select("person", "sum(amount)");

sumWithdrawals.cache(); // Used in next query too
sumWithdrawals.show();

// Q2. Person with maximum total withdrawals
long maxTotal = sumWithdrawals
    .agg(max("sum(amount)"))
    .first()
    .getLong(0);

Dataset<Row> maxWithdrawals = sumWithdrawals
    .filter(col("sum(amount)").equalTo(maxTotal));
maxWithdrawals.show();

// Q3. Accounts with negative balance
Dataset<Row> totWithdrawals = withdrawals
    .groupBy("account")
    .sum("amount")
    .drop("person");

Dataset<Row> totDeposits = deposits
    .groupBy("account")
    .sum("amount")
    .drop("person");

// Left outer join to find negative accounts
Dataset<Row> negativeAccounts = totWithdrawals
    .join(totDeposits, 
          totDeposits.col("account").equalTo(totWithdrawals.col("account")), 
          "left_outer")
    .filter(
        totDeposits.col("sum(amount)").isNull()
            .and(totWithdrawals.col("sum(amount)").gt(0))
        .or(totWithdrawals.col("sum(amount)").gt(totDeposits.col("sum(amount)")))
    )
    .select(totWithdrawals.col("account"));

negativeAccounts.show();
```

#### 4.4 Join Operations

**Join Types:**
- `inner` (default): Returns rows with matching keys in both DataFrames
- `left_outer` / `left`: Returns all rows from left, with nulls for non-matching right rows
- `right_outer` / `right`: Returns all rows from right, with nulls for non-matching left rows
- `full_outer` / `outer`: Returns all rows from both, with nulls where no match

**Code Example: Join with Aliases (Java)**

```java
Dataset<Row> friends = spark.read().csv("friends.csv");
Dataset<Row> closure = spark.read().csv("closure.csv");

// Use aliases to disambiguate columns
Dataset<Row> joined = friends.alias("friends")
    .join(
        closure.alias("closure"),
        col("friends.friend").equalTo(col("closure.user"))
    );

// Select with qualified names
Dataset<Row> result = joined.select(
    col("friends.user").alias("user"),
    col("closure.friend").alias("friend")
);
```

***

### 5. Spark Streaming and Structured Streaming

**Stream processing** focuses on dynamic data sets that continuously change, typically modeling data continuously appended to a stream (e.g., sensor readings). The goal is to continuously update computation results with low latency.

#### 5.1 Spark Streaming (DStreams)

The original streaming API, **Spark Streaming**, uses a **micro-batch** approach. It splits input streams into small chunks of data (micro batches) that are processed by the Spark engine as sequences of RDDs.

The main abstraction is the **Discretized Stream (DStream)**, which is internally a sequence of RDDs.

**Code Example: Basic DStream Setup (Java)**

```java
final SparkConf conf = new SparkConf()
    .setMaster("local[*]")
    .setAppName("StreamingWordCount");

// Create streaming context with 1-second batch interval
final JavaStreamingContext sc = new JavaStreamingContext(conf, Durations.seconds(1));
sc.sparkContext().setLogLevel("ERROR");

// Create DStream from socket
JavaDStream<String> lines = sc.socketTextStream("localhost", 9999);

// Process the stream...

sc.start();
sc.awaitTermination();
sc.close();
```

##### 5.1.1 Stateful Operations

DStreams support stateful operators that preserve internal state across RDDs.

1.  **`updateStateByKey`:** Maintains a key-value store where the value (state) for a key is updated based on new input elements.
2.  **`mapWithState`:** Embeds the key-value store update logic inside a transformation, allowing the definition of a new output and updated state based on the previous state and incoming batch.
3.  **Windows:** Apply transformations over a sliding window of data, defined by **window length** and **sliding interval** (frequency of evaluation). This is the most common stateful operation.

**Code Example: Streaming Word Count with Windows (Java)**

```java
final JavaStreamingContext sc = new JavaStreamingContext(conf, Durations.seconds(1));
sc.sparkContext().setLogLevel("ERROR");

final JavaPairDStream<String, Integer> counts = sc
    .socketTextStream("localhost", 9999)
    .window(Durations.seconds(10), Durations.seconds(5)) // 10s window, 5s slide
    .map(String::toLowerCase)
    .flatMap(line -> Arrays.asList(line.split(" ")).iterator())
    .mapToPair(s -> new Tuple2<>(s, 1))
    .reduceByKey((a, b) -> a + b);

counts.foreachRDD(rdd -> rdd.collect().forEach(System.out::println));

sc.start();
sc.awaitTermination();
sc.close();
```

**Code Example: mapWithState for Cumulative Counts (Java)**

```java
final JavaStreamingContext sc = new JavaStreamingContext(conf, Durations.seconds(1));
sc.checkpoint("/tmp/"); // Required for stateful operations

// Define state update function
final Function3<String, Optional<Integer>, State<Integer>, Tuple2<String, Integer>> 
    mapFunction = (word, count, state) -> {
        final int sum = count.orElse(0) + (state.exists() ? state.get() : 0);
        state.update(sum);
        return new Tuple2<>(word, sum);
    };

// Initialize empty state
final List<Tuple2<String, Integer>> initialList = new ArrayList<>();
final JavaPairRDD<String, Integer> initialRDD = sc.sparkContext().parallelizePairs(initialList);

// Apply stateful transformation
final JavaMapWithStateDStream<String, Integer, Integer, Tuple2<String, Integer>> state = sc
    .socketTextStream("localhost", 9999)
    .map(String::toLowerCase)
    .flatMap(line -> Arrays.asList(line.split(" ")).iterator())
    .mapToPair(word -> new Tuple2<>(word, 1))
    .mapWithState(StateSpec.function(mapFunction).initialState(initialRDD));

state.foreachRDD(rdd -> rdd.collect().forEach(System.out::println));

sc.start();
sc.awaitTermination();
sc.close();
```

#### 5.2 Structured Streaming

**Structured Streaming** builds on the Spark SQL engine. The programming model treats a stream as an **unbounded table** to which new data is continuously appended. Developers write standard batch-like queries, and the engine handles continuous, incremental execution.

##### Output Modes
1.  **Complete Mode:** Returns the entire result table after each change.
2.  **Append Mode:** Returns only the new rows appended since the last evaluation.
3.  **Update Mode:** Returns only the rows that were updated (appended or modified) since the last evaluation.

**Code Example: Structured Streaming Word Count (Java)**

```java
// 1. Initialize Spark Session (for Spark SQL/Structured Streaming)
SparkSession spark = SparkSession.builder()
    .master("local[*]")
    .appName("StructuredStreamingWordCount")
    .getOrCreate();
spark.sparkContext().setLogLevel("ERROR");

// 2. Define Input Stream (Unbounded Table) from a socket
Dataset<Row> lines = spark.readStream()
    .format("socket")
    .option("host", "localhost")
    .option("port", 9999)
    .load();

// 3. Process the Data (similar to batch processing)
// Convert DataFrame to Dataset<String>
Dataset<String> words = lines
    .as(Encoders.STRING())
    .flatMap(
        (FlatMapFunction<String, String>) x -> Arrays.asList(x.split(" ")).iterator(), 
        Encoders.STRING()
    );

// Generate running word count
Dataset<Row> wordCounts = words.groupBy("value").count();

// 4. Define Output Query (Action)
StreamingQuery query = wordCounts.writeStream()
    .outputMode("update") // Output only updated rows
    .format("console")
    .start();

query.awaitTermination();
spark.close();
```

**Code Example: Stream Enrichment with Static Data (Java)**

This example joins streaming events with static reference data:

```java
// Static reference data: product classifications
List<StructField> productClassificationFields = new ArrayList<>();
productClassificationFields.add(DataTypes.createStructField("product", DataTypes.StringType, false));
productClassificationFields.add(DataTypes.createStructField("classification", DataTypes.StringType, false));
StructType productClassificationSchema = DataTypes.createStructType(productClassificationFields);

Dataset<Row> productsClassification = spark.read()
    .option("header", "false")
    .option("delimiter", ",")
    .schema(productClassificationSchema)
    .csv("product_classification.csv");

// Streaming input: product events
Dataset<Row> inStream = spark.readStream()
    .format("socket")
    .option("host", "localhost")
    .option("port", 9999)
    .load();

Dataset<Row> inStreamDF = inStream.toDF("product");

// Join stream with static data and aggregate
StreamingQuery query = inStreamDF
    .join(productsClassification, 
          inStreamDF.col("product").equalTo(productsClassification.col("product")))
    .groupBy("classification")
    .count()
    .writeStream()
    .outputMode("update")
    .format("console")
    .start();

query.awaitTermination();
```

#### 5.3 Time Management in Streams

Stream processing must manage time, which is complicated in a distributed setting due to different node clocks.

1.  **Processing Time:** The wall clock time at a given processing node. This is often inconsistent.
2.  **Event Time:** A logical timestamp attached to a data element by its original source (e.g., a sensor). Event time is usually more significant as it is deterministic.

Since data may arrive out of order (late data), Structured Streaming uses **retraction**: it outputs results and later changes (retracts) them if late data arrives. Spark allows defining a **watermark** to specify how long to keep old state around to correct aggregates for late data before discarding it.

**Code Example: Windowing with Event Time (Java)**

```java
// Create DataFrame from rate source (generates records with timestamp)
Dataset<Row> inputRecords = spark.readStream()
    .format("rate")
    .option("rowsPerSecond", 1)
    .load();

// Define watermark for late data handling
inputRecords.withWatermark("timestamp", "1 hour");

// Window aggregation: 30-second windows, 10-second slide
StreamingQuery query = inputRecords
    .withColumn("value", col("value").mod(100))
    .withColumn("timestamp", date_format(col("timestamp"), "HH:mm:ss"))
    .groupBy(
        window(col("timestamp"), "30 seconds", "10 seconds"),
        col("value")
    )
    .count()
    .writeStream()
    .outputMode("update")
    .format("console")
    .start();

query.awaitTermination();
```

***

### 6. Spark MLlib - Machine Learning

**Spark MLlib** is Spark's machine learning library, providing tools for:
- Classification and regression
- Clustering
- Collaborative filtering
- Dimensionality reduction
- Feature extraction and transformation
- ML pipelines

#### 6.1 ML Pipelines

MLlib uses the concept of **Pipelines** to chain multiple transformations and estimators together.

**Key Concepts:**
- **Transformer:** Transforms one DataFrame into another (e.g., feature extraction)
- **Estimator:** Fits on a DataFrame to produce a Transformer (e.g., a learning algorithm)
- **Pipeline:** Chains multiple Transformers and Estimators as a single workflow

**Code Example: K-Means Clustering (Java)**

```java
SparkSession spark = SparkSession.builder()
    .master("local[*]")
    .appName("KMeans")
    .getOrCreate();
spark.sparkContext().setLogLevel("ERROR");

// Load data in LIBSVM format
Dataset<Row> data = spark.read()
    .format("libsvm")
    .load("sample_libsvm_data.txt");

// Train k-means model with 2 clusters
KMeans kmeans = new KMeans().setK(2).setSeed(1L);
KMeansModel model = kmeans.fit(data);

// Make predictions
Dataset<Row> predictions = model.transform(data);

// Evaluate clustering
ClusteringEvaluator evaluator = new ClusteringEvaluator();
double silhouette = evaluator.evaluate(predictions);
System.out.println("Silhouette with squared euclidean distance = " + silhouette);

// Show cluster centers
Vector[] centers = model.clusterCenters();
System.out.println("Cluster Centers: ");
for (Vector center : centers) {
    System.out.println(center);
}
```

**Code Example: Decision Tree Classification Pipeline (Java)**

```java
// Load data
Dataset<Row> data = spark.read()
    .format("libsvm")
    .load("sample_libsvm_data.txt");

// Index labels
StringIndexerModel labelIndexer = new StringIndexer()
    .setInputCol("label")
    .setOutputCol("indexedLabel")
    .fit(data);

// Index features
VectorIndexerModel featureIndexer = new VectorIndexer()
    .setInputCol("features")
    .setOutputCol("indexedFeatures")
    .setMaxCategories(4) // Features with >4 values are continuous
    .fit(data);

// Split data: 70% training, 30% test
Dataset<Row>[] splits = data.randomSplit(new double[]{0.7, 0.3});
Dataset<Row> trainingData = splits[0];
Dataset<Row> testData = splits[1];

// Train Decision Tree
DecisionTreeClassifier dt = new DecisionTreeClassifier()
    .setLabelCol("indexedLabel")
    .setFeaturesCol("indexedFeatures");

// Convert indexed labels back to original
IndexToString labelConverter = new IndexToString()
    .setInputCol("prediction")
    .setOutputCol("predictedLabel")
    .setLabels(labelIndexer.labelsArray()[0]);

// Chain in Pipeline
Pipeline pipeline = new Pipeline()
    .setStages(new PipelineStage[]{labelIndexer, featureIndexer, dt, labelConverter});

// Train model
PipelineModel model = pipeline.fit(trainingData);

// Make predictions
Dataset<Row> predictions = model.transform(testData);
predictions.select("predictedLabel", "label", "features").show(5);

// Evaluate
MulticlassClassificationEvaluator evaluator = new MulticlassClassificationEvaluator()
    .setLabelCol("indexedLabel")
    .setPredictionCol("prediction")
    .setMetricName("accuracy");
double accuracy = evaluator.evaluate(predictions);
System.out.println("Test Error = " + (1.0 - accuracy));
```

#### 6.2 Estimators, Transformers, and Parameters

**Code Example: Logistic Regression with Parameter Tuning (Java)**

```java
// Prepare training data
List<Row> dataTraining = Arrays.asList(
    RowFactory.create(1.0, Vectors.dense(0.0, 1.1, 0.1)),
    RowFactory.create(0.0, Vectors.dense(2.0, 1.0, -1.0)),
    RowFactory.create(0.0, Vectors.dense(2.0, 1.3, 1.0)),
    RowFactory.create(1.0, Vectors.dense(0.0, 1.2, -0.5))
);
StructType schema = new StructType(new StructField[]{
    new StructField("label", DataTypes.DoubleType, false, Metadata.empty()),
    new StructField("features", new VectorUDT(), false, Metadata.empty())
});
Dataset<Row> training = spark.createDataFrame(dataTraining, schema);

// Create LogisticRegression estimator
LogisticRegression lr = new LogisticRegression();

// Set parameters using setters
lr.setMaxIter(10).setRegParam(0.01);

// Fit model
LogisticRegressionModel model1 = lr.fit(training);
System.out.println("Model 1 was fit using parameters: " + model1.parent().extractParamMap());

// Alternative: Use ParamMap
ParamMap paramMap = new ParamMap()
    .put(lr.maxIter().w(20))
    .put(lr.maxIter(), 30)  // This overwrites previous
    .put(lr.regParam().w(0.1), lr.threshold().w(0.55));

ParamMap paramMap2 = new ParamMap()
    .put(lr.probabilityCol().w("myProbability"));

ParamMap paramMapCombined = paramMap.$plus$plus(paramMap2);

// Fit with combined params
LogisticRegressionModel model2 = lr.fit(training, paramMapCombined);

// Make predictions
List<Row> dataTest = Arrays.asList(
    RowFactory.create(1.0, Vectors.dense(-1.0, 1.5, 1.3)),
    RowFactory.create(0.0, Vectors.dense(3.0, 2.0, -0.1)),
    RowFactory.create(1.0, Vectors.dense(0.0, 2.2, -1.5))
);
Dataset<Row> test = spark.createDataFrame(dataTest, schema);

Dataset<Row> results = model2.transform(test);
for (Row r : results.select("features", "label", "myProbability", "prediction").collectAsList()) {
    System.out.println("(" + r.get(0) + ", " + r.get(1) + ") -> prob=" + r.get(2) + ", prediction=" + r.get(3));
}
```

***

### 7. Important Imports and Dependencies

**Common Java Imports for Spark:**

```java
// Core Spark
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaPairRDD;
import scala.Tuple2;

// Spark SQL
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.types.*;
import static org.apache.spark.sql.functions.*;

// Streaming (DStreams)
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaPairDStream;
import org.apache.spark.streaming.State;
import org.apache.spark.streaming.StateSpec;

// Structured Streaming
import org.apache.spark.sql.streaming.StreamingQuery;
import org.apache.spark.sql.streaming.StreamingQueryException;
import org.apache.spark.sql.Encoders;
import org.apache.spark.api.java.function.FlatMapFunction;

// Machine Learning
import org.apache.spark.ml.Pipeline;
import org.apache.spark.ml.PipelineModel;
import org.apache.spark.ml.PipelineStage;
import org.apache.spark.ml.classification.*;
import org.apache.spark.ml.clustering.*;
import org.apache.spark.ml.evaluation.*;
import org.apache.spark.ml.feature.*;
import org.apache.spark.ml.linalg.Vector;
import org.apache.spark.ml.linalg.Vectors;
import org.apache.spark.ml.linalg.VectorUDT;

// Utilities
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
```

***

### 8. Common Patterns and Best Practices

#### 8.1 Caching Strategy
- Use `.cache()` when an RDD/DataFrame is used multiple times
- In iterative algorithms, cache the dataset that changes each iteration
- Use `.unpersist()` to free memory when cached data is no longer needed

#### 8.2 Partitioning
- Default partitioning is based on HDFS block size or number of cores
- Use `.repartition(n)` to increase partitions (causes shuffle)
- Use `.coalesce(n)` to decrease partitions (avoids shuffle when possible)

#### 8.3 Broadcast Variables
- Use for small lookup tables that need to be accessed by all workers
- Example: `Broadcast<Map<String, String>> bcMap = sc.broadcast(lookupMap);`

#### 8.4 Accumulators
- Use for distributed counters/sums
- Example: `LongAccumulator counter = sc.sc().longAccumulator("myCounter");`

#### 8.5 Performance Tips
- Minimize shuffles (avoid `groupByKey`, prefer `reduceByKey`)
- Use DataFrames/Datasets over RDDs when possible (catalyst optimizer)
- Filter early to reduce data volume
- Persist intermediate results that are reused
- Use appropriate serialization (Kryo over Java serialization)

***

### 9. Quick Reference Cheat Sheet

#### RDD Operations
```java
// Transformations (Lazy)
rdd.map(x -> x * 2)                              // Transform each element
rdd.flatMap(x -> Arrays.asList(x.split(" ")))   // Transform and flatten
rdd.filter(x -> x > 10)                          // Keep elements matching condition
rdd.mapToPair(x -> new Tuple2<>(x, 1))          // Create key-value pairs
rdd.reduceByKey((a, b) -> a + b)                // Aggregate by key
rdd.groupByKey()                                 // Group values by key (avoid if possible)
rdd.distinct()                                   // Remove duplicates
rdd.union(otherRDD)                              // Combine two RDDs
rdd.join(otherRDD)                               // Join by key

// Actions (Eager - trigger computation)
rdd.collect()                                    // Return all elements to driver
rdd.count()                                      // Count elements
rdd.first()                                      // Get first element
rdd.take(n)                                      // Get first n elements
rdd.reduce((a, b) -> a + b)                     // Aggregate all elements
rdd.foreach(x -> System.out.println(x))         // Apply function to each element
rdd.saveAsTextFile("path")                      // Save to file

// Persistence
rdd.cache()                                      // Cache in memory
rdd.persist(StorageLevel.MEMORY_AND_DISK())     // Custom persistence
rdd.unpersist()                                  // Remove from cache
```

#### DataFrame/Dataset Operations
```java
// Reading Data
spark.read().csv("file.csv")
spark.read().json("file.json")
spark.read().parquet("file.parquet")
spark.read().format("libsvm").load("file.txt")
spark.read().option("header", "true").schema(schema).csv("file.csv")

// Transformations
df.select("col1", "col2")                        // Select columns
df.filter(col("age").gt(18))                    // Filter rows
df.where(col("age").gt(18))                     // Same as filter
df.groupBy("col1")                              // Group by column
df.agg(sum("col1"), avg("col2"))                // Aggregate
df.orderBy(desc("col1"))                        // Sort
df.join(df2, df.col("id").equalTo(df2.col("id"))) // Join
df.union(df2)                                   // Union
df.distinct()                                    // Remove duplicates
df.withColumn("new", expr("col1 + col2"))       // Add/replace column
df.withColumnRenamed("old", "new")              // Rename column
df.drop("col1")                                 // Drop column
df.dropDuplicates("col1", "col2")               // Drop duplicates by columns

// Actions
df.show()                                        // Display results
df.show(10)                                      // Display 10 rows
df.count()                                       // Count rows
df.collect()                                     // Return all rows to driver
df.first()                                       // Get first row
df.take(n)                                       // Get first n rows
df.write().csv("output.csv")                    // Write to CSV
df.write().parquet("output.parquet")            // Write to Parquet

// Common Functions (import static org.apache.spark.sql.functions.*)
col("colName")                                   // Reference column
expr("col1 + col2")                             // SQL expression
lit(value)                                       // Literal value
when(condition, value).otherwise(other)         // Conditional
sum("col"), avg("col"), max("col"), min("col")  // Aggregations
count("col"), countDistinct("col")              // Count functions
window(col("timestamp"), "10 seconds", "5 seconds") // Time window
date_format(col("timestamp"), "yyyy-MM-dd")     // Format date
```

#### Streaming Operations
```java
// DStream (Spark Streaming)
sc.socketTextStream(host, port)                 // Create from socket
dstream.window(windowLength, slideInterval)     // Window operation
dstream.foreachRDD(rdd -> { /* process */ })    // Process each RDD
dstream.mapWithState(StateSpec.function(func))  // Stateful mapping

// Structured Streaming
spark.readStream().format("socket")             // Create stream
  .option("host", host).option("port", port).load()
query.writeStream()                             // Write stream
  .outputMode("update")                         // complete/append/update
  .format("console")                            // console/parquet/etc
  .start()
query.awaitTermination()                        // Wait for termination
df.withWatermark("timestamp", "10 minutes")    // Handle late data
```

#### Machine Learning
```java
// Pipeline Components
new StringIndexer().setInputCol().setOutputCol()    // Index strings
new VectorIndexer().setInputCol().setOutputCol()    // Index features
new Pipeline().setStages(stages)                    // Create pipeline
model.fit(trainingData)                             // Train model
model.transform(testData)                           // Make predictions

// Common Algorithms
new KMeans().setK(k)                                // K-means clustering
new LogisticRegression()                            // Logistic regression
new DecisionTreeClassifier()                        // Decision tree
new RandomForestClassifier()                        // Random forest

// Evaluation
new ClusteringEvaluator()                           // Clustering evaluation
new MulticlassClassificationEvaluator()             // Classification evaluation
```

#### Configuration Examples
```java
// Master URLs
"local"                // Run locally with 1 thread
"local[4]"            // Run locally with 4 threads
"local[*]"            // Run locally with all available cores
"spark://host:7077"   // Connect to Spark standalone cluster

// Common Configurations
conf.set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
conf.set("spark.sql.shuffle.partitions", "200")
conf.set("spark.default.parallelism", "100")
```

***

### Analogy: Spark Processing

You can think of Spark's execution model—where RDDs are transformed lazily into a DAG that is only computed upon an Action—like a factory floor manager (the Driver) setting up a complex manufacturing process (the DAG). The manager writes down a series of instructions (Transformations) but doesn't start production. When a customer places an order (the Action), the manager breaks the instructions into efficient steps (Stages), assigns small jobs (Tasks) to various workers (Executors) based on where the raw materials (data partitions) are located, and only then does the actual manufacturing begin. If a worker fails, the detailed recipe (Lineage) allows the manager to restart only the failed piece, ensuring the final product remains correct.