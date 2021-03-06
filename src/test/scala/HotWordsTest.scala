import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
  * Created by li on 16/6/29.
  */
object HotWordsTest extends App {

  val conf = new SparkConf().setAppName("test").setMaster("local")
  val sc = new SparkContext(conf)

  val a = ("t1", Array("程序员","专业"))
  val b = ("t2", Array("测试", "方面"))
  val c = ("t3", Array("方面","代码"))

//  val d = ("url1", Array("程序", "程序员", "专业"))
//  val e = ("url2", Array("代码", "程序"))
//  val f = ("url3", Array("专业", "代码", "方面", "专业"))
//  val g1 = ("url4", Array("专业", "代码", "专业"))
//  val h1 = ("url2", Array("代码", "程序", "程序员"))


  val d = Array("程序", "程序员", "专业")
  val e = Array("代码", "程序")
  val f = Array("专业", "代码", "方面")
  val g1 = Array("专业", "代码", "专业")
  val h1 = Array("代码", "程序")


  val dArray = Array(d, e, f, g1, h1) //文档集
  val aArray = sc.parallelize(dArray)

  val cArray = Array(a, b, c) //社区集


  val g = ("url1", "银行 代码 金融")
  val h = ("url2", "电力 爬虫 计算机")
  val k = ("url3", "电力 代码 计算机")



  //  def getWordRank(eventWordsList: RDD[(String, List[String])],
  //                  newStateFilter: RDD[(String, String)]): RDD[(String, Seq[(String, Int)])] = {
  //
  //    val temp = newStateFilter.map(_._2).map(x => x.split(",")).collect
  //    val eventWord = mutable.HashMap[String, Int]()
  //
  //    val res = eventWordsList.map {
  //      event =>{
  //        val key = event._1
  //        val eventWords = event._2
  //        eventWords.foreach {
  //          word => {
  //            for(item <- temp.indices) {
  //              if (temp(item).contains(word)) {
  //                val value = eventWord.getOrElseUpdate(word, 0)
  //                eventWord.update(word, value + 1)
  //              }
  //            }
  //          }
  //        }
  //
  //        val a = (key, eventWord.toSeq)
  //        print(a._1)
  //        a._2.foreach(x => println((x._1, x._2)))
  //
  //        (key, eventWord.toSeq)
  //      }
  //    }
  //
  //    res
  //  }


  def getWordRank(sc: SparkContext,
                  eventWordsList: RDD[(String, List[String])],
                  newStateFilter: RDD[(String, String)]): Seq[(String, (String, Int))] = {

    val res = new ArrayBuffer[(String, (String, Int))]

    val aa = eventWordsList.map(_._2).flatMap(x => x).distinct().collect()

    val cc = newStateFilter.map(line => (line._1, line._2.split(",").distinct))

    val bb = cc.map {
      word =>
        val url = word._1
        val filteredWords = word._2.filter(x => aa.contains(x)).map((_, 1))
        (url, filteredWords)
    }.cache()

    val wordDf = bb.flatMap(_._2).reduceByKey(_ + _).collect()
    val broadcastWordDf = sc.broadcast(wordDf)

    val eventWordsRepositoryMap = mutable.HashMap[String, Int]()
    broadcastWordDf.value.foreach{
      line => {
        val words = line._1
        val df = line._2
        eventWordsRepositoryMap.put(words, df)
      }
    }

    eventWordsList.collect().foreach {
      line => {
        val url = line._1
        line._2.foreach {
          word => {
            val temp = eventWordsRepositoryMap.get(word).get
            res.+=((url, (word, temp)))
          }
        }
      }
    }

    res.toSeq
  }




//  def entityWordsGet(propertyTable: RDD[(String, String)]): (RDD[(Seq[String], (String, Int))],
//    RDD[(Seq[String], (String, Int))],
//    RDD[(Seq[String], (String, Int))]) = {
//
//    val entity = propertyTable.map {
//      line => {
//        val url = line._1
//        val stockTemp = line._2.split(" ")(0)
//        val industryTemp = line._2.split(" ")(1)
//        val sectionTemp = line._2.split(" ")(2)
//        ((stockTemp, industryTemp, sectionTemp ), url)
//
//      }
//    }
//
//    val stockUrl = entity.map(x => (x._1._1, x._2)).groupByKey()
//    val industryUrl = entity.map(x => (x._1._2, x._2)).groupByKey()
//    val sectionUrl = entity.map(x => (x._1._3, x._2)).groupByKey()
//
//    val stockUrlCount = stockUrl.map(line =>{
//      val urlList = line._2.toSeq
//      val stockProperty = line._1
//      val df = line._2.size
//      (urlList, (stockProperty , df))
//    })
//
//    val sectionUrlCount = sectionUrl.map(line =>{
//      (line._2.toSeq, (line._1, line._2.size))
//    })
//
//    val industryUrlCount = industryUrl.map(line =>{
//      (line._2.toSeq, (line._1, line._2.size))
//    })
//
//    val stockUrlList = stockUrlCount.flatMap(
//      line =>{
//        val list = new ListBuffer[(String, (String, Int))]
//
//        line._1.foreach(x => {
//
//          list.+=((x,line._2))
//        })
//
//        list
//      }
//    ).collect()
//
//    val industryUrlList = industryUrlCount.flatMap(
//      line =>{
//        val list = new ListBuffer[(String, (String, Int))]
//
//        line._1.foreach(x => {
//
//          list.+=((x,line._2))
//        })
//
//        list
//      }
//    ).collect()
//
//    val sectionUrlList = sectionUrlCount.flatMap(
//      line =>{
//        val list = new ListBuffer[(String, (String, Int))]
//
//        line._1.foreach(x => {
//
//          list.+=((x,line._2))
//        })
//
//        list
//      }
//    ).collect()
//
//    val propertyLibList = stockUrlList.++:(industryUrlList).++:(sectionUrlList).toMap
//
//    (stockUrlCount, industryUrlCount, sectionUrlCount)
//  }

  def entityWordsUnionEventWords(entityWords: (RDD[(String, Iterable[String], Int)],
    RDD[(String, Iterable[String], Int)],
    RDD[(String, Iterable[String], Int)]),
                                 eventWords: Seq[(String, (String, Int))]): (Seq[(String, String, Int)],
    Seq[(String, String, Int)],
    Seq[(String, String, Int)]) = {

    // 通过url合并实体词和事件词,整理成[属性,Seq(词项, 词频)]的格式
    val stockWordsUrl = entityWords._1
    val industryWordsUrl = entityWords._2
    val sectionWordsUrl = entityWords._3

    val stockWords = new ArrayBuffer[(String, String, Int)]
    stockWordsUrl.collect().foreach {
      line => {
        eventWords.foreach (
          x => {
            if (line._2.toArray.contains(x._1)) {
              stockWords.+=((line._1, x._2._1, x._2._2))
            }
          }
        )
      }
    }

    val industryWords = new ArrayBuffer[(String, String, Int)]
    industryWordsUrl.collect().foreach {
      line => {
        eventWords.foreach (
          x => {
            if (line._2.toArray.contains(x._1)) {
              industryWords.+=((line._1, x._2._1, x._2._2))
            }
          }
        )
      }
    }

    val sectionWords = new ArrayBuffer[(String, String, Int)]
    sectionWordsUrl.collect().foreach {
      line => {
        eventWords.foreach (
          x => {
            if (line._2.toArray.contains(x._1)) {
              sectionWords.+=((line._1, x._2._1, x._2._2))
            }
          }
        )
      }
    }

    (stockWords.toSeq, industryWords.toSeq, sectionWords.toSeq)
  }

  /**
    *
    * @param hotWords
    * @param preHotWords
    */
  def bayesianAverage(hotWords: RDD[(String, Int)],
                      preHotWords: RDD[(String, Int)]): Seq[(String, Float)] ={

    //Atp(w): 当前词频
    val hotWord = hotWords.map(_._1)

    //Btp(w): 历史词频
    val preHotWord = preHotWords.map(_._1)

    val wordLib = hotWords.++(preHotWords)

    //TpSum: 词频和
    val wordLibArray = wordLib.reduceByKey(_ + _).collect()


    //TpAvg:词频和的平均
    val tpSum = wordLibArray.map(_._2).sum
    val tpAvg = tpSum.toFloat / wordLibArray.length

    //Atp(w)/TpSum 当前词频与词频和比值
    val resultMap = new mutable.HashMap[String, Float]
    val atp = hotWords.collect().toMap
    val wordLibMap = wordLibArray.toMap
    wordLibMap.foreach {
      line =>{
        if (atp.contains(line._1)){
          val temp = atp.get(line._1).get
          val item = temp.toFloat / line._2
          resultMap.put(line._1, item)
        } else {
          resultMap.put(line._1, 0f)
        }
      }
    }

    //R(avg) 当前词频与词频和比值的平均值
    val rAvg = resultMap.values.toArray.sum / resultMap.values.size

    // 热度计算
    val result = new mutable.HashMap[String, Float]
    wordLibMap.foreach {
      line => {
        val res1 = wordLibMap.get(line._1).get
        val res2 = resultMap.get(line._1).get
        val value = (res1 * res2 + tpAvg * rAvg) / (res1 + tpAvg)
        result.put(line._1, value)
      }
    }

    result.toSeq
  }


  def newtonCooling(hotWords: RDD[(String, Int)],
                    preHotWords: RDD[(String, Int)],
                    timeRange: Int): Array[(String, Float)] ={

    val wordLib = hotWords.++(preHotWords)

    //TpSum: 词频和
    val wordLibArray = wordLib.reduceByKey(_ + _).collect().toMap

    val result = new mutable.HashMap[String, Float]

    wordLibArray.map{
      line => {
        val keywords = line._1
        val atp = line._2
        val btp = wordLibArray.get(keywords).get - atp
        val item = math.log((atp + 1) / (btp + 1) / timeRange).toFloat
        result.put(keywords, item)
      }
    }

    result.toArray
  }

//  /**
//    * 排序算法主程序入口
//    * @param hotWords 当前热词
//    * @param preHotWords 前段时间的热词
//    * @param timeRange 时间间隔
//    * @param alpha 贝叶斯平均的权重, 一般0.7
//    * @param beta 牛顿冷却算法的权重, 一般0.3
//    * @return 热词候选词和计算出的热度
//    * @author Li Yu
//    */
//  def run(hotWords: RDD[(String, Int)],
//          preHotWords: RDD[(String, Int)],
//          timeRange: Int,
//          alpha: Double,
//          beta: Double): Array[(String, Float)] ={
//
//    val result = mutable.HashMap[String, Float]()
//
//    val bayesianAverageResult = bayesianAverage(hotWords, preHotWords).toMap
//
//    val newtonCoolingResult = newtonCooling(hotWords, preHotWords, timeRange).toMap
//
//    bayesianAverageResult.foreach {
//      line => {
//        val key = line._1
//        val value = line._2
//        val temp = (alpha * value) + (beta * newtonCoolingResult.get(key).get)
//        result.put(key, temp.toFloat)
//      }
//    }
//
//    result.toArray
//  }




//  def fileProcessing(sc: SparkContext, dir: String): Array[(String, (String, Int))] = {
//
//
//
//  }

  val data = sc.parallelize(List(a, b, c))
  val data2 = sc.parallelize(List(d, e, f))
  val data3 = sc.parallelize(List(g, h, k))

  val a1 = List(("程序", 1.0), ("程序员", 4.0), ("测试", 2), ("代码", 3), ("程序", 4))
  val a2 = List(("测试", 2.0), ("程序", 4.0), ("银行", 10))
  val data4 = sc.parallelize(a1)
  val data5 = sc.parallelize(a2)

  val a3 = List(("六合彩", 1106.0), ("直播", 469.0), ("男同志", 3410.0), ("李宇春", 442.0), ("无耻", 2713.0), ("俞思远", 149.0), ("演唱会", 741.0), ("好男儿", 0.0), ("董文华", 2246.0), ("婚纱", 653.0), ("太正宵", 1.0), ("敢死队", 119.0))
  val a4 = List(("六合彩", 1702.0), ("直播", 769.0), ("男同志", 3925.0), ("李宇春", 649.0), ("无耻", 2939.0), ("俞思远", 331.0), ("演唱会", 749.0), ("好男儿", 441.0), ("董文华", 4672.0), ("婚纱", 1650.0), ("太正宵", 5.0), ("敢死队", 75.0))
  val data6 = (a3).toArray
  val data7 = (a4).toArray

  // 事件词词频
//  val reslut1 = getWordRank(sc, data, data2)

  // 实体词词频
//  val result2 =  entityWordsGet(data3)

  // 实体词事件词揉合
//  val result3 = entityWordsUnionEventWords(result2, reslut1)
//  result3._1.foreach(x => println("stock" + x._1, x._2, x._3))
//  result3._2.foreach(x => println("industry" + x._1, x._2, x._3))
//  result3._3.foreach(x => println("section" + x._1, x._2, x._3))

//  val result5 = bayesianAverage(data4, data5)
//  result5.foreach(x => println("re55: " + x))
//  println("length1: " + result5.length)

//  val result6 = newtonCooling(data4, data5, 1)
//  result6.foreach(x => println("res66: " + x))
//  println("length2: " + result6.length)

//  val result7 = run(data4, data5, 1, 0.7, 0.3)
//  val result8 = HotDegreeCalculate.run(data7, data6, 1, 0.7, 0.3)
//
//  result8.foreach(x => println("result8:" + x))
//
  val dir = "/Users/li/kunyan/NaturalLanguageProcessing/src/test/resources/"
//  fileIO.saveAsTextFile(dir, result8)
//
//  val res = fileIO.readFromFile(dir)
//  res.foreach(x => println(x))

//
//  val reslut9 = CommunityFrequencyStatistics.communityFrequencyStatisticsRDD(aArray, cArray)
//  reslut9.foreach(x => println("result9" + x))


//  HotDegreeCalculation()

//  HotDegreeCalculation.run(dir, aArray, cArray, 1, 0.7, 0.3)

  val arr = sc.parallelize(Array(1, 2, 3 , 4, 5))
  val result = arr.flatMap(x => Array(x + 1)).cache()
  val resu = arr.map(x => Array(x + 1)).collect()
  result.foreach(println)

  resu.foreach(println)

}