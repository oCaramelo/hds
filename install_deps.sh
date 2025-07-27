#!/usr/bin/env bash

mvn install:install-file -Dfile=dependencies/gson-2.10.1.jar -DgroupId="com.google.code.gson" -DartifactId="gson" -Dversion="2.10.1" -Dpackaging="jar"
mvn install:install-file -Dfile=dependencies/gson-2.12.1.jar -DgroupId="com.google.code.gson" -DartifactId="gson" -Dversion="2.12.1" -Dpackaging="jar"
mvn install:install-file -Dfile=dependencies/besu-datatypes-24.12.2.jar -DgroupId="org.hyperledger.besu" -DartifactId="besu-datatypes" -Dversion="24.12.2" -Dpackaging="jar"
mvn install:install-file -Dfile=dependencies/evm-24.12.2.jar -DgroupId="org.hyperledger.besu" -DartifactId="evm" -Dversion="24.12.2" -Dpackaging="jar"
mvn install:install-file -Dfile=dependencies/tuweni-bytes-2.3.1.jar -DgroupId="org.apache.tuweni" -DartifactId="tuweni-bytes" -Dversion="2.3.1" -Dpackaging="jar"
mvn install:install-file -Dfile=dependencies/tuweni-units-2.3.1.jar -DgroupId="org.apache.tuweni" -DartifactId="tuweni-units" -Dversion="2.3.1" -Dpackaging="jar"
mvn install:install-file -Dfile=dependencies/core-24.12.2.jar -DgroupId=org.web3j -DartifactId=web3j-core -Dversion=24.12.2 -Dpackaging=jar
mvn install:install-file -Dfile=dependencies/crypto-23.1.3.jar -DgroupId=org.web3j -DartifactId=web3j-crypto -Dversion=23.1.3 -Dpackaging=jar
mvn install:install-file -Dfile=dependencies/web3j-utils-4.0.4.jar -DgroupId=org.web3j -DartifactId=utils -Dversion=4.0.4 -Dpackaging=jar
mvn install:install-file -Dfile=dependencies/bcprov-jdk18on-1.80.jar -DgroupId=org.bouncycastle -DartifactId=bcprov-jdk18on -Dversion=1.80 -Dpackaging=jar
mvn install:install-file -Dfile=dependencies/gnark-1.1.1.jar -DgroupId=org.gnark -DartifactId=gnark -Dversion=1.1.1 -Dpackaging=jar
mvn install:install-file -Dfile=dependencies/slf4j-api-2.0.16.jar -DgroupId=org.slf4j -DartifactId=slf4j-api -Dversion=2.0.16 -Dpackaging=jar
mvn install:install-file -Dfile=dependencies/guava-33.4.0-jre.jar -DgroupId=com.google.guava -DartifactId=guava -Dversion=33.4.0-jre -Dpackaging=jar
mvn install:install-file -Dfile=dependencies/rlp-24.12.2.jar -DgroupId=org.hyperledger.besu -DartifactId=rlp -Dversion=24.12.2 -Dpackaging=jar
mvn install:install-file -Dfile=dependencies/caffeine-3.2.0.jar -DgroupId=com.github.ben-manes.caffeine -DartifactId=caffeine -Dversion=3.2.0 -Dpackaging=jar
mvn install:install-file -Dfile=dependencies/secp256k1-1.1.1.jar -DgroupId=org.bitcoin -DartifactId=secp256k1 -Dversion=1.1.1 -Dpackaging=jar
mvn install:install-file -Dfile=dependencies/jna-5.16.0.jar -DgroupId=net.java.dev.jna -DartifactId=jna -Dversion=5.16.0 -Dpackaging=jar












