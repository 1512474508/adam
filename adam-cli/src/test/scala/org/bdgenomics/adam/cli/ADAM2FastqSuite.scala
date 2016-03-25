/**
 * Licensed to Big Data Genomics (BDG) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The BDG licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.bdgenomics.adam.cli

import com.google.common.io.Files
import java.io.File
import org.apache.spark.rdd.RDD
import org.bdgenomics.adam.rdd.ADAMContext._
import org.bdgenomics.adam.util.ADAMFunSuite
import org.bdgenomics.formats.avro.AlignmentRecord

class Adam2FastqSuite extends ADAMFunSuite {

  sparkTest("convert SAM to paired FASTQ") {
    val readsFilepath = resourcePath("bqsr1.sam")

    // The following fastq files were generated by Picard's SamToFastq

    // First generate mapped on SAM file (sorted by readName to make the comparison easier)

    // samtools view -H adam-core/src/test/resources/bqsr1.sam > adam-core/src/test/resources/bqsr1-readnamesorted.sam
    // samtools view -F 4 adam-core/src/test/resources/bqsr1.sam | sort -k1 >> adam-core/src/test/resources/bqsr1-readnamesorted.sam

    // java -jar picard.jar
    // SamToFastq 
    // I=adam-core/src/test/resources/bqsr1-readnamesorted.sam 
    // FASTQ=adam-core/src/test/resources/bqsr1-r1.fq 
    // SECOND_END_FASTQ=adam-core/src/test/resources/bqsr1-r2.fq 
    // VALIDATION_STRINGENCY=SILENT 

    // VALIDATION_STRINGENCY=SILENT is necessary since they are unpaired reads and this matches the ADAM default

    val fastq1Path = resourcePath("bqsr1-r1.fq")
    val fastq2Path = resourcePath("bqsr1-r2.fq")

    val outputDir = Files.createTempDir()
    val outputFastqR1File = outputDir.getAbsolutePath + "/bqsr1-r1.fq"
    val outputFastqR2File = outputDir.getAbsolutePath + "/bqsr1-r2.fq"

    // Only looking at mapped reads
    // This is because Picard and ADAM disagree on setting negative strand on unmapped reads
    // Picard allows unmapped reads to set the negative strand flag and therefore reverse-complemented on output
    val reads: RDD[AlignmentRecord] =
      sc
        .loadAlignments(readsFilepath)
        .rdd
        .filter(r => r.getReadMapped != null && r.getReadMapped)

    reads.saveAsFastq(outputFastqR1File, Some(outputFastqR2File), sort = true)

    val goldR1Reads =
      scala.io.Source.fromFile(new File(fastq1Path)).getLines().toSeq

    val goldR2Reads =
      scala.io.Source.fromFile(new File(fastq2Path)).getLines().toSeq

    val outputR1Reads = scala.io.Source.fromFile(new File(outputFastqR1File + "/part-00000")).getLines().toSeq
    val outputR2Reads = scala.io.Source.fromFile(new File(outputFastqR2File + "/part-00000")).getLines().toSeq

    assert(outputR1Reads.length === goldR1Reads.length)
    assert(outputR2Reads.length === goldR2Reads.length)

    outputR1Reads.zip(goldR1Reads).foreach(kv => assert(kv._1 === kv._2))
    outputR2Reads.zip(goldR2Reads).foreach(kv => assert(kv._1 === kv._2))

  }

}
