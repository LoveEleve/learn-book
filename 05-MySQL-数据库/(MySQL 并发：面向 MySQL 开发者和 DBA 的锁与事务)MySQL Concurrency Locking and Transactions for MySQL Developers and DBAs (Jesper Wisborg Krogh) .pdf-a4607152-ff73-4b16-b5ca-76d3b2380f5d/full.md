![](images/e18b880d74b5b35effa7678a192c59bf21ae81532190f6aa565bf95ecba65c91.jpg)

# MySQL Concurrency

Locking and Transactions for MySQL

Developers and DBAs

Jesper Wisborg Krogh

# MySQL Concurrency

Locking and Transactions for MySQL Developers and DBAs

Jesper Wisborg Krogh

# MySQL Concurrency: Locking and Transactions for MySQL Developers and DBAs

Jesper Wisborg Krogh

Hornsby, NSW, Australia

ISBN-13 (pbk): 978-1-4842-6651-9

https://doi.org/10.1007/978-1-4842-6652-6

ISBN-13 (electronic): 978-1-4842-6652-6

# Copyright $^ ©$ 2021 by Jesper Wisborg Krogh

This work is subject to copyright. All rights are reserved by the Publisher, whether the whole or part of the material is concerned, specifically the rights of translation, reprinting, reuse of illustrations, recitation, broadcasting, reproduction on microfilms or in any other physical way, and transmission or information storage and retrieval, electronic adaptation, computer software, or by similar or dissimilar methodology now known or hereafter developed.

Trademarked names, logos, and images may appear in this book. Rather than use a trademark symbol with every occurrence of a trademarked name, logo, or image we use the names, logos, and images only in an editorial fashion and to the benefit of the trademark owner, with no intention of infringement of the trademark.

The use in this publication of trade names, trademarks, service marks, and similar terms, even if they are not identified as such, is not to be taken as an expression of opinion as to whether or not they are subject to proprietary rights.

While the advice and information in this book are believed to be true and accurate at the date of publication, neither the authors nor the editors nor the publisher can accept any legal responsibility for any errors or omissions that may be made. The publisher makes no warranty, express or implied, with respect to the material contained herein.

Managing Director, Apress Media LLC: Welmoed Spahr

Acquisitions Editor: Jonathan Gennick

Development Editor: Laura Berendson

Coordinating Editor: Jill Balzano

Cover image designed by Freepik (www.freepik.com)

Distributed to the book trade worldwide by Springer Science+Business Media LLC, 1 New York Plaza, Suite 4600, New York, NY 10004. Phone 1-800-SPRINGER, fax (201) 348-4505, e-mail orders-ny@springersbm.com, or visit www.springeronline.com. Apress Media, LLC is a California LLC and the sole member (owner) is Springer Science $^ +$ Business Media Finance Inc (SSBM Finance Inc). SSBM Finance Inc is a Delaware corporation.

For information on translations, please e-mail booktranslations@springernature.com; for reprint, paperback, or audio rights, please e-mail bookpermissions@springernature.com.

Apress titles may be purchased in bulk for academic, corporate, or promotional use. eBook versions and licenses are also available for most titles. For more information, reference our Print and eBook Bulk Sales web page at http://www.apress.com/bulk-sales.

Any source code or other supplementary material referenced by the author in this book is available to readers on GitHub via the book’s product page, located at www.apress.com/9781484266519. For more detailed information, please visit http://www.apress.com/source-code.

Printed on acid-free paper

To my wife Ann-Margrete – Thanks for the patience and support.

# Table of Contents

About the Author .………..Xxii   
About the Technical Reviewer xv   
Acknowledgments …mmmm xvii   
Introduction xix   
Chapter 1: Introduction  1   
Why Are Locks Needed? 1   
Lock Levels 2   
Locks and Transactions�� 3   
Examples��� 3   
Prerequisites for the concurrency_book.generate Module ��������������� 4   
Installing the concurrency_book.generate Module 5   
Getting Information���������������   
Loading Test Data ��������������   
Executing a Workload 9   
Test Data: The world Schema� 15   
Schema�� 15   
Installation � 16   
Test Data: The sakila Schema � 20   
Schema���� 20   
Installation � 26   
Test Data: The employees Schema � 27   
Schema���� 27   
Installation � 29   
Summary������������� 30

# Chapter 2: Monitoring Locks and Mutexes 31

The Performance Schema� 31   
Metadata and Table Locks� 32   
Data Locks� 36   
Synchronization Waits 40   
Statement and Error Tables �� 43

The sys Schema� 48   
Status Counters and InnoDB Metrics � 48   
Querying the Data� 49   
Configuring the InnoDB Metrics�� 51

InnoDB Lock Monitor and Deadlock Logging ������������������ 54   
InnoDB Mutexes and Semaphores � 61   
Summary����������� 65

# Chapter 3: Monitoring InnoDB Transactions 67

Information Schema INNODB_TRX� 67   
InnoDB Monitor ����� 75   
INNODB_METRICS and sys.metrics� 77   
Summary����������� 81

# Chapter 4: Transactions in the Performance Schema  83

Transaction Events and Their Statements� 83   
Transaction Summary Tables �������������� 94   
Summary��� 96

# Chapter 5: Lock Access Levels 97

Shared Locks 97   
Exclusive Locks� 100   
Intention Locks���� 102   
Lock Compatibility�������������� 102   
Summary������������� 103

# Chapter 6: High-Level Lock Types …. 105

User-Level Locks��������������� 105

Flush Locks ��� 109

Metadata Locks�� 111

Explicit Table Locks� 115

Implicit Table Locks��� 116

Backup Locks� 118

Log Locks� 121

Summary������������ 122

# Chapter 7: InnoDB Locks 123

Record Locks and Next-Key Locks� 123

Gap Locks � �������������� 126

Predicate and Page Locks� 128

Insert Intention Locks 130

Auto-Increment Locks 132

Mutexes and RW-Lock Semaphores � 133

Summary��� 139

# Chapter 8: Working with Lock Conflicts …………… 141

Contention-Aware Transaction Scheduling (CATS)� 142

InnoDB Data Lock Compatibility��������������� 143

Metadata and Backup Lock Wait Timeouts 143

InnoDB Lock Wait Timeouts� 145

Deadlocks ��������������� 146

InnoDB Mutex and Semaphore Waits� 153

Summary������������ 155

# Chapter 9: Reducing Locking Issues 157

Transaction Size and Age 157

Indexes� 158

Record Access order � 162

# Table of Cont ent s

Transaction Isolation Levels 162   
Configuration������������� 166   
Resource Partitioning � 166   
Disabling the InnoDB Adaptive Hash Index������������������� 167   
Reducing Priority of Metadata Write Locks 168

Preemptive Locking ����������� 169

Summary������������ 170

# Chapter 10: Indexes and Foreign Keys 171

Indexes� � 171   
Primary vs. Secondary Indexes � ����������������������� 172   
Ascending vs. Descending Indexes 176   
Unique Indexes ������������������������� 180

Foreign Keys 184   
DML Statement� 185   
DDL Statement 189

Summary������������ 190

# Chapter 11: Transactions 193

Transactions and ACID 193   
Atomicity 193   
Consistency � ����������������������� �������������������������������� 194   
Isolation 195   
Durability ��������������������������� 195

Impact of Transactions ����������� 196

Locks � 196   
Undo Logs���������������� 198

Group Commit ���������� 200

Summary������������ 201

# Chapter 12: Transaction Isolation Levels 203

Serializable 204

Repeatable Read 207

Read Committed 213

Read Uncommitted 217

Summary���� 218

# Chapter 13: Case Study: Flush Locks 219

The Symptoms ��� 220

The Cause ����� 221

The Setup��� 222

The Investigation� 223

The Solution �� 228

The Prevention �� 229

Summary������������ 230

# Chapter 14: Case Study: Metadata and Schema Locks 231

The Symptoms ��� 231

The Cause ���� 232

The Setup����� 232

The Investigation� 233

The Solution 244

The Prevention ����������������� 245

Summary������������ 246

# Chapter 15: Case Study: Record-Level Locks 247

The Symptoms ��� 247

The Cause ���� 252

The Setup����� 252

The Investigation� 253

The Solution ��� 256

The Prevention �� 257

Summary����� 257

# Table of Cont ent s

# Chapter 16: Case Study: Deadlocks 259

The Symptoms 259

The Cause ���������������������� 260

The Setup��� 261

The Investigation� 263

The Solution 274

The Prevention 274

Summary�� 275

# Chapter 17: Case Study: Foreign Keys 277

The Setup��� 277

The Discussion 281

Errors and High-Level Monitoring�� 281

Lock Metrics ������������������������������������� �������������������������������������� 283

Metadata Lock Contention� 285

InnoDB Lock Contention 292

The Solution and Prevention 294

Summary������������ 295

# Chapter 18: Case Study: Semaphores 297

The Symptoms 297

The Cause ����������������� 298

The Setup� 299

The Investigation� ��������� 303

The InnoDB RW-Lock Metrics 303

InnoDB Monitor and Mutex Monitor� 305

Determining the Workload� 309

The Solution and Prevention � � 312

Disabling the Adaptive Hash Index ��� 312

Increase the Number of Hash Index Parts ������ 317

Other Solutions ���� 318

Summary������������ � 318

# Appendix A: References 321

Tables and Views �������������� 321   
Lock Information�� 322   
Metadata Object Types ��������� 323   
Metadata Lock Types� 324   
Transaction Information� 325   
Statement Information� 326   
Wait Information ���������������� 329   
Table I/O Information 330   
File I/O Information � 332   
Error Information 333   
Status Variables and InnoDB Metrics 334

InnoDB Monitor Sections�� � 334

# Appendix B: MySQL Shell Module 337

Prerequisites 337   
Installation 338   
The help() and show() Methods� 339   
Loading Test Data� 343   
Executing a Workload� 345   
Module Structure ���� 352

Library Files� 353   
Workloads Directory �������� 360   
Defining Workloads ��� 360   
Global Keys ������������������ 362   
Queries and Completions 364   
Investigations � ������������� 366   
Summary������������������� 368

# ndeX.…………………………………………………………… 369

# About the Author

![](images/51dcb298505abbe2310c786dd6dcd5381b87bfb036b086fa3f4e5202df465904.jpg)

Jesper Wisborg Krogh has worked with MySQL databases since 2006 both as a SQL developer and a database administrator and for more than 8 years as part of the Oracle MySQL Support team. He currently works as a database reliability engineer for Okta. He has spoken at MySQL Connect, Oracle OpenWorld, and Oracle Developer Live on several occasions. In addition to his books, Jesper regularly blogs on MySQL topics and has authored approximately 800 documents in the Oracle Knowledge Base. He has contributed to the sys schema and four Oracle Certified Professional (OCP) exams for MySQL 5.6–8. Jesper holds a PhD in computational chemistry; lives in Sydney,

Australia; and enjoys spending time outdoors walking, traveling, and reading. His areas of expertise include MySQL Cluster, MySQL Enterprise Backup (MEB), performance tuning, and the performance and sys schemas.

# About the Technical Reviewer

![](images/47a08a3cc0338f09d4f06e3797e857a61c39abed1b41276bd3a081b91d352de6.jpg)

Charles Bell conducts research in emerging technologies. He is a member of the Oracle MySQL Development team and is a senior software developer for the MySQL Enterprise Backup team. He lives in a small town in rural Virginia with his loving wife. He received his Doctor of Philosophy in Engineering from Virginia Commonwealth University in 2005.

Charles is an expert in the database field and has

extensive knowledge and experience in software development and systems engineering. His research interests include 3D printers, microcontrollers, three-dimensional printing, database systems, software engineering, high-availability systems, cloud, and sensor networks. He spends his limited free time as a practicing Maker, focusing on microcontroller projects and refinement of three-dimensional printers.

# Acknowledgments

I would first of all like to say thank you to all of those from the Apress team that have made this book possible. In particular, I would like to shout out Jonathan Gennick who came up with the idea, Jill Balzano who coordinated the work, Laura Berendson for her work behind the scenes, and Creapzylene Roma for catching my linguistic slipups.

The knowledge shared in this book has not materialized out of nothing. Thanks to Charles Bell for providing – as always – a thorough review with constructive feedback and suggestions for improvements. Jakub Lopuszanski has been helpful with details on InnoDB locking. I would also like to thank all my colleagues over the years as they have all been part of my journey learning how MySQL works through mentorship, teaching me, asking me good questions, and general discussions. A special thank you to Edwin Desouza and Frédéric Descamps (better known as Lefred) for their assistance.

Last but not least, thanks to my wife Ann-Margrete for her patience and support while I wrote this book. Without you, it would not have been possible to write this book.

# Introduction

When working with databases, locks and transactions are some of the most difficult and misunderstood topics. This book aims at improving your understanding of these two concepts, how they work, how you can investigate them, and how you can improve your workload, so it works the best with them. This is achieved through a combination of discussing monitoring, the lock and transaction theory, and a series of case studies.

MySQL is famous for its support for storage engines. However, this book exclusively covers the InnoDB storage engine, and only MySQL 8 is considered. That said, most of the discussion also applies to older versions of MySQL, and in general, it is mentioned when a feature is new in MySQL 8 or that MySQL 8 has a different behavior compared to older versions.

# Book Audience

The book has been written for developers and database administrators who have experience working with MySQL and want to expand their knowledge of how locks and transactions work in the realm of MySQL concurrency.

# Examples and the Book’s GitHub Repository

I have tried to add as many examples and outputs from examples as possible. Some of the examples are quite short, some are quite long. In either case, I hope you are able to follow them and reproduce the effect or result demonstrated. At the same time, please do bear in mind that by nature there is often randomness involved, and the exact outcome of the examples may depend on how the tables and data have been used prior to the example. In other words, you may get different results even if you did everything right. This particularly applies to numbers that relate to lock ids, memory locations, mutexes/ semaphores, timings, and the like.

Examples that are long or produce outputs that are either long or wide have been added to this book’s GitHub repository. This includes some of the figures that may be hard to read with the image size that the page format allows.

Note The link to the repository can be found from the book’s home page at www. apress.com/gp/book/9781484266519. It can also be found directly at www. github.com/Apress/mysql-concurrency.

To make it easier to reproduce the examples and to provide example queries that can be used to examine the issue the test demonstrates, a module written in Python for use in MySQL Shell is also included in the book’s GitHub repository. The basic installation and usage instructions are covered in Chapter 1 and the full documentation in Appendix B.

The GitHub repository will also be the home of the errata for the book once that is created. I will use the errata not only to communicate errors in the book but also to provide updates when bug fixes and new features in MySQL 8 cause changes to book content. If necessary, I will also update the examples in the repository to reflect the behavior in the newer releases. For these reasons, I recommend that you keep an eye on the repository.

# Book Structure

I have attempted to keep each chapter relatively self-contained with the aim that you can use the book as a reference book. The drawback of this choice is that there is some duplication of information from time to time. This is particularly evident in the case studies that repeat some of the information discussed in earlier chapters. This was a deliberate choice, and I hope it helps you to reduce the amount of page flipping to find the information you need.

The book is divided into 18 chapters and two appendixes. Chapter 1 provides an introduction, Chapters 2–4 cover monitoring of locks and transactions, Chapters 5–10 discuss locks, Chapters 11–12 contain information about transactions, and finally Chapters 13–18 go through six case studies. The appendixes contain references for monitoring locks and transactions and for the MySQL Shell module provided with the book.

Chapter 1, “Introduction”: This introductory chapter covers some high-level concepts as well as introduces the MySQL Shell module for reproducing the example and the test data used in this book.   
• Chapter 2, “Monitoring Locks”: This chapter covers how you can monitor locks using the Performance Schema, the sys schema, status counters, and InnoDB metrics. There is also information on how to use the InnoDB lock monitor and the deadlock information in the InnoDB monitor and how to obtain information about mutex and semaphore contention.   
• Chapter 3, “Monitoring InnoDB Transactions”: This chapter primarily shows how you can use the information_schema.INNODB_ TRX view to investigate InnoDB transaction. The transaction list in the InnoDB monitor and transaction-related InnoDB metrics are also covered.   
• Chapter 4, “Transactions in the Performance Schema”: This chapter continues where the previous stopped by going through the transaction information in the Performance Schema and how you can find the statements for a transaction.   
Chapter 5, “Lock Access Levels”: This chapter goes through shared, exclusive, and intention locks as well as shows which of these are compatible with each other.   
Chapter 6, “High-Level Lock Types”: This chapter covers the locks that work at a higher level than records. These are mainly locks handled outside the scope of the storage engines and include userlevel locks, metadata locks, flush locks, and table-level locks. The new MySQL 8 backup and log locks are also included.   
• Chapter 7, “InnoDB Locks”: This chapter goes to the record locks used by InnoDB. These include plain record locks, gab locks, predicate locks, insert intention locks, auto-increment locks, as well as mutexes and rw-lock semaphores.   
• Chapter 8, “Working with Lock Conflicts”: This chapter explains what happens when lock conflicts occur from a discussion of the contention-aware transaction scheduling (CATS) used internally to

prioritize locks and a discussion of lock compatibility to lock wait timeouts and deadlocks.

• Chapter 9, “Reduce Locking Issues”: This chapter covers how you can reduce lock contention and the effects of the contention in your system. Methods include reducing the transaction size and age, using indexes, accessing records in the same order for concurrent tasks, changing the transaction isolation level, and more.   
• Chapter 10, “Indexes and Foreign Keys”: This chapter considers the effect of indexes and foreign keys on locking in detail. Do unique indexes require less locks than non-unique indexes? Do foreign keys cause more locks? The answer is yes, and this chapter explains why that is the case and gives examples of the differences.   
• Chapter 11, “Transactions”: This chapter discusses what transactions are and how they help handle concurrent workloads. The chapter also covers the impact of transactions and how the group commit feature helps reduce the impact of persisting committed transactions.   
Chapter 12, “Transaction Isolation Levels”: This chapter goes through the four transaction isolation levels supported by InnoDB with a discussion on how each level affects locking and data consistency.   
Chapter 13, “Case Study: Flush Locks”: This chapter sets the database up, so there is flush lock contention, and then goes through analyzing the issue and providing a solution and discusses how to prevent the issue.   
• Chapter 14, “Case Study: Metadata and Schema Locks”: This chapter takes on another common lock issue by studying a situation with metadata locks.   
Chapter 15, “Case Study: Record-Level Locks”: This chapter performs an investigation into InnoDB record-level locks and discusses how to resolve the lock issue and reduce the chance of encountering them.

Chapter 16, “Case Study: Deadlocks”: This chapter goes through an investigation of a deadlock with details of analyzing the deadlock information from the InnoDB monitor output.   
Chapter 17, “Case Study: Foreign Keys”: This chapter covers an advanced lock scenario caused by foreign keys, involving both metadata and InnoDB record locks.   
Chapter 18, “Case Study: Semaphores”: This chapter sets up a test case triggering semaphore contention and performs an investigation of the issue.   
Appendix A, “References”: This appendix provides an overview of the resources for finding information related to the topics discussed in this book. The resources primarily consist of tables in the Performance Schema and views in the sys schema including the possible values of the OBJECT_TYPE and LOCK_TYPE columns of the performance_schema.metadata_locks table. Some Information Schema resources are also included, and there is a list of sections in the output from the InnoDB monitor.   
Appendix B, “MySQL Shell Script Reproducing Lock Scenarios”: This appendix is the reference for the Python module included with the book. This includes a discussion of how to install and use the module as well as how the code is organized in case you want to extend it with your own workloads.

# Introduction

Concurrency and locking are some of the most complex topics when it comes to databases. A query that usually executes fast and without problems may suddenly take much longer or fail with an error when the conditions are “just right,” so locking or contention becomes a problem. You may ask yourself why locks are around when they can cause such problems. This and the next 11 chapters will try to explain that as well as how you best deal with them. The last six chapters of the book go through six case studies putting the information together in realistic scenarios together with analysis and how to avoid or reduce the issue.

In this chapter, you will first learn why locks are important despite the problems they can cause. Then the relationship with transactions will be explained. The rest of the chapter introduces how examples are used in this book as well as the world, sakila, and employees databases which are used throughout this book for the examples.

# Why Are Locks Needed?

It can seem like a perfect world where locking in databases is not needed. The price will however be so high that only few use cases can use that database, and it is impossible to avoid locks for a general-purpose database such as MySQL. If you do not have locking, you cannot have any concurrency. Imagine that only one connection is ever allowed to the database (you can argue that it itself is a lock and thus the system is not lock-free anyway) – that is not very useful for most applications.

Note Often what is called a lock in MySQL is really a lock request which can be in a granted or pending state.

When you have several connections executing queries concurrently, you need some way to ensure that the connections do not step on each other’s toes. That is where locks enter the picture. You can think of locks in the same way as traffic signals in road traffic (Figure 1-1) that regulate access to the resources to avoid accidents. In a road intersection, it is necessary to ensure that two cars do not cross each other’s path and collide.

![](images/c1408d2256748baddc07a8bcc3202fc8f33d8310733b710748ed471d85f0d7b9.jpg)  
Figure 1-1. Locks in databases are similar to traffic lights

In a database, it is necessary to ensure two queries’ access to the data does not conflict. As there are different levels of controlling the access to an intersection – yielding, stop signs, and traffic lights – there are different lock types in a database.

# Lock Levels

Locks in MySQL come in several flavors acting at different levels in MySQL ranging from user-level locks to record locks. At the highest level are the user-level locks which can protect whole code paths in the application and any object inside the database. In the

middle there are locks that operate on the database objects. These include metadata locks that protect the metadata of the tables as well as table locks that protect all data in the table. Common for the user-level and table-level locks is that they are implemented at the SQL layer of the database. The high-level locks are discussed in Chapter 6.

At the lowest level are the locks implemented by the storage engines. By nature, these locks depend on the storage engine you use. As InnoDB is by far the most used storage engine in MySQL (and the default), this book covers the InnoDB-specific locks. InnoDB includes locks on the records, which are the easiest to understand, as well as more difficult concepts such as gap locks, next key locks, predicate locks, and insert intention locks. Additionally, there are mutexes and semaphores (this also happens at the level of the SQL layer). The InnoDB-specific locks and mutexes/semaphores are covered in Chapter 7.

# Locks and Transactions

It can seem odd at first to combine the topics of locks and transactions into one book about concurrency. However, they are strongly related as you will see several examples of in this book. Some locks are held for the duration of a transaction, so it is important to understand how transactions work and how to monitor them.

The concept of transaction isolation level also plays an important role when working with locks. The isolation level influences both which locks are taken and how long a time they are held.

Chapters 3 and 4 cover how you can monitor transactions, and Chapters 11 and 12 cover how transactions work, their impact, and the transaction isolation levels.

# Examples

Throughout the book there are examples that help illustrate the topic being discussed or set up a situation that you can investigate. Except for Chapters 17 and 18, all statements required to reproduce the test are listed. In general, you will need more than one connection for the examples, so the prompts for the queries have been set to indicate which connection to use for which queries when that is important. For example, Connection 1> means that the query should be executed by the first of your connections.

All the examples in this book have been executed in MySQL Shell. For brevity the prompt in the examples is mysql> except when the connection is important or when the language mode is not SQL. The examples will however also work from the old mysql command-line client.

Tip If you are unfamiliar with MySQL Shell, then it is a second-generation MySQL command-line client with support for both SQL, Python, and JavaScript. It also comes with several built-in utilities including tools for managing MySQL InnoDB Cluster and traditional replication topologies. For an introduction to MySQL Shell, see the user guide at https://dev.mysql.com/doc/mysql-shell/en/ or the book Introducing MySQL Shell (Apress) by Charles Bell (www.apress.com/ gp/book/9781484250822).

Additionally, this book comes with a Python module – concurrency_book. generate – that can be imported into MySQL Shell and used to reproduce all but the simplest examples. The rest of this section describes how to use the MySQL Shell module. The content here is an excerpt of Appendix B which contains a longer reference for the module including how to implement your own examples.

Note By nature, some of the data in the examples will be different for each execution. This is particularly the case for ids and memory addresses and similar. So, do not expect to get identical results for all details when you try to reproduce the examples.

# Prerequisites for the concurrency_book.generate Module

The most important requirement to use the MySQL Shell module provided with this book is that you are using MySQL Shell 8.0.20 or later. This is a strict requirement as the module primarily uses the shell.open_session() method to create the connections needed for the test cases. This method was only introduced in release 8.0.20. The advantage of shell.open_session() over the mysql.get_classic_session() and mysqlx.get_session() is that open_session() works transparently with both the classic MySQL protocol and the new X protocol.

If you for some reason are stuck with an older version of MySQL Shell, you can update the test cases to include the protocol setting (see Defining Workloads in Appendix B) to explicitly specify which protocol to use.

It is also required that a connection already exists from MySQL Shell to MySQL Server as the module uses the URI of that connection when creating the additional connections required for the example.

The examples have been tested with MySQL Server 8.0.21; however, most of the examples will work with older releases and some even with MySQL 5.7. That said, it is recommended to use MySQL Server 8.0.21 or later.

# Installing the concurrency_book.generate Module

To use the module, you need to download the files in the concurrency_book directory from this book’s GitHub repository (the link can be found on the book’s home page at www.apress.com/gp/book/9781484266519). The easiest is to clone the repository or to download the ZIP file with all the files using the menu shown in Figure 1-2.

![](images/730f7b472ad48fa21e59436384481675ecf311497844c14e861289df414c12f3.jpg)  
Figure 1-2. The GitHub menu for cloning or downloading the repository

Click on the clipboard icon to copy the URL used to clone the repository using the Git software of your system or use the Download ZIP link to download a ZIP file of the repository. You are free to choose any path as the location of the files as long as the structure below the concurrency_book directory is kept. For this discussion,

it is assumed you have cloned the repository or unzipped the file to C:\Book\mysqlconcurrency, so the generate.py file is in the directory C:\Book\mysql-concurrency\ concurrency_book\.

To be able to import the module in MySQL Shell, open or create the mysqlshrc.py file. MySQL Shell searches in four places for the file. On Microsoft Windows, the paths are in the order they are searched:

1. %PROGRAMDATA%\MySQL\mysqlsh\   
2. %MYSQLSH_HOME%\shared\mysqlsh\   
3. <mysqlsh binary path>\   
4. %APPDATA%\MySQL\mysqlsh\

# On Linux and Unix

1. /etc/mysql/mysqlsh/   
2. $MYSQLSH_HOME/shared/mysqlsh/   
3. <mysqlsh binary path>/   
4. $HOME/.mysqlsh/

All four paths are always searched, and if the file is found in multiple locations, each file will be executed. This means that the last found file takes precedence if the files affect the same variables. If you make changes meant for you personally, the best place to make the changes is in the fourth location. The path in step 4 can be overridden with the MYSQLSH_USER_CONFIG_HOME environment variable.

You need to ensure the mysqlshrc.py file adds the directory with the module to the Python search path, and optionally you can add an import statement to make the module available when you start MySQL Shell. An example of the mysqlshrc.py file is

import sys

sys.path.append('C:\\Book\\mysql-concurrency')

import concurrency_book.generate

The double backslashes are for Windows; on Linux and Unix, you do not need to escape the slashes that separate the path elements. If you do not include the import in the mysqlshrc.py file, you will need to execute it in MySQL Shell before you can use the module.

# Getting Information

The module includes two methods that return information on how to use the module. One is the help()method which provides information on how to use the module:

mysql-py> concurrency_book.generate.help()

There is also the show() method which lists the workloads that the run() method can execute and the schemas that the load() method can load:

mysql-py> concurrency_book.generate.show()

The workloads are named after the code listings in the book, for example, the workload named “Listing 6-1” implements the example in Listing 6-1.

Before you can start executing the workloads, you need to load some test data which the module can do for you as well.

# Loading Test Data

The concurrency_book.generate module supports loading the employees, sakila, and world example databases into your MySQL instance. For the employees database, you can optionally choose a version with partitions. The world database is the most important for this book followed by the sakila database. The employees database is only used for the case study in Chapter 18. Each of the three schemas is described in more detail later in this chapter.

Note If the schema exists, it will be dropped as part of the load job. This effectively means that load() resets the schema.

You load a schema with the load() method which optionally takes the name of the schema you want to load. If you do not provide a schema name, then you will be prompted. Listing 1-1 shows an example of loading the world schema.

# Listing 1-1. Loading the world schema

mysql-py> concurrency_book.generate.load()

Available Schema load jobs:

# Name

Description

1 employees

The employee database

2 employees partitioned

The employee database with partitions

3 sakila

The sakila database

4 world

The world database

Choose Schema load job (# or name - empty to exit): 4

2020-07-20 21:27:15.221340 0 [INFO] Downloading https://downloads.

mysql.com/docs/world.sql.zip to C:\Users\myuser\AppData\Roaming\mysql_ concurrency_book\sample_data\world.sql.zip

2020-07-20 21:27:18.159554 0 [INFO] Processing statements in world.sql

2020-07-20 21:27:27.045219 0 [INFO] Load of the world schema completed

Available Schema load jobs:

# Name

Description

1 employees

The employee database

2 employees partitioned

The employee database with partitions

3 sakila

The sakila database

4 world

The world database

Choose Schema load job (# or name - empty to exit):

The load() method downloads the file with the schema definition, if it does not already have it. The downloaded file is stored in %APPDATA\mysql_concurrency_book\ sample_data\ on Microsoft Windows and in $\$ \{ H0 M E \}$ .mysql_concurrency_book/ sample_data/ on other platforms. If you want the file re-downloaded, delete it from that directory.

Tip A s only relatively low-level network routines are available in MySQL Shell’s Python, downloading the employees database may fail if you have a slow or unstable connection. One option – other than installing the schema manually – is to download https://github.com/datacharmer/test_db/archive/ master.zip and save it in the sample_data directory. After that, the load() method will pick it up and not attempt to download it again.

If you only want to load a single schema, you can specify the name as an argument to load(). This can be particularly useful when initiating a schema load as a command given directly on the command line when invoking MySQL Shell, for example

shell> mysqlsh --user $\bf \tilde { = }$ myuser --py -e "concurrency_book.generate. load('world')"

When you are done loading the schemas you need, you can reply with an empty answer to exit. You are now ready to execute the workloads.

Note If the load process crashes complaining about the file, for example, that it is not a ZIP file, then it suggests the file is corrupted or incomplete. In that case, delete the file, so it is re-downloaded, or try to download the file manually using your browser.

# Executing a Workload

You execute a workload with the run() method. If you specify the name of the known workload, then that workload will be executed immediately. Otherwise, the available workloads are listed, and you are prompted for the workload. You can in this case specify the workload either by the number (e.g., 15 for Listing 6-1) or by the name. When using the name, the number of spaces between Listing and the listing number does not matter as long as there is at least one space. When you choose the workload using the prompt, you can choose another workload once the previous has completed.

After the workload has completed, for several of the workloads, you will be given a list of suggestions for investigations you can do. This can, for example, be to query the locks held by the connections used in the example. The investigations are meant as inspiration, and you are encouraged to explore the workload using your own queries. Some of the investigations are also used in the discussion of the example. Listing 1-2 shows an example of executing a workload using the prompt.

# Listing 1-2. Executing a workload using the prompt

mysql-py> concurrency_book.generate.run()

Available workloads:

# Name

Description

1 Listing 2-1

Example use of the metadata_locks table

2 Listing 2-2

Example of using the table_handles table

3 Listing 2-3

Using the data_locks table

14 Listing 5-2

Example of obtaining exclusive locks

15 Listing 6-1

A deadlock for user-level locks

Choose workload (# or name - empty to exit): 15

Password for connections: ********

2020-07-20 20:50:41.666488 0 [INFO] Starting the workload Listing 6-1

****************************************************

* *

* Listing 6-1. A deadlock for user-level locks *

* *

****************************************************

-- Connection Processlist ID Thread ID Event ID

1 105 249 6

2 106 250 6

```sql
-- Connection 1   
Connection 1> SELECT GET_LOCK('my_lock_1'，-1); +   
|GET_LOCK('my_lock_1'，-1) |   
+   
|1   
+   
1 row in set (0.0003 sec)   
-- Connection 2   
Connection 2> SELECT GET_LOCK('my_lock_2'，-1);   
+   
|GET_LOCK('my_lock_2'，-1) |   
+   
|1   
+   
1 row in set (0.0003 sec)   
Connection 2> SELECT GET_LOCK('my_lock_1'，-1);
```

```sql
-- Connection 1  
Connection 1> SELECT GET_LOCK('my_lock_2', -1);  
ERROR: 3058: Deadlock found when trying to get user-level lock; try rolling back transaction/releasing locks and restarting lock acquisition. 
```

Available investigations:

# Query

```sql
1 SELECT * FROM performance_schema_metadata_locks WHERE object_type = 'USER LEVEL LOCK' AND owner_thread_id IN (249, 250) 
```

```sql
2 SELECT thread_id, event_id, sql_text, mysql_errno, returnedsqlstate, message_text, errors, warnings 
```

Chapter 1 Introductio n   
FROM performance_schema.events_statementsthistory WHERE thread_id $=$ 249 AND event_id $>6$ ORDER BY event_id   
Choose investigation (# - empty to exit): 2   
-- Investigation #2   
-- Connection 3   
Connection 3> SELECT thread_id, event_id, sql_text, mysql_errno, returned_sqlstate, message_text, errors, warnings FROM performance_schema.events_statementsthistory WHERE thread_id $=$ 249 AND event_id $>6$ ORDER BY event_id\G   
**********1. row*******1. row*******2. row*******3. row*******4. row*******5. row*******6. row*******7. row*******8. row*******9. row*******10. row*******11. row*******12. row*******13. row*******14. row*******15. row*******16. row*******17. row*******18. row*******19. row*******20. row*******21. row*******22. row*******23. row*******24. row*******25. row*******26. row*******27. row*******28. row*******29. row*******30. row*******31. row*******32. row*******33. row*******34. row*******35. row*******36. row*******37. row*******38. row*******39. row*******40. row*******41. row*******42. row*******43. row*******44. row*******45. row*******46. row*******47. row*******48. row*******49. row*******50. row*****51. row*****52. row*****53. row*****54. row*****55. row*****56. row*****57. row*****58. row*****59. row*****60. row*****61. row*****62. row*****63. row*****64. row*****65. row*****66. row*****67. row*****68. row*****69. row*****70. row*****71. row*****72. row*****73. row*****74. row*****75. row*****76. row*****77. row*****78. row*****79. row*****80. row*****81. row*****82. row*****83. row*****84. row*****85. row*****86. row*****87. row*****88. row*****89. row*****90. row*****91. row*****92. row*****93. row*****94. row*****95. row*****96. row*****97. row*****98. row*****99. row*****100

*************************** 3. row ***************************

```yaml
thread_id: 249
    event_id: 9
    sql_text: SHOWWarnings
    mysql_errno: 0
    returned_sqlstate: NULL
    message_text: NULL
        errors: 0
        warnings: 0
3 rows in set (0.0009 sec) 
```

Available investigations:

# Query

Choose investigation (# - empty to exit):

2020-07-20 20:50:46.749971 0 [INFO] Completing the workload Listing 6-1

-- Connection 1

Connection 1> SELECT RELEASE_ALL_LOCKS();

+- -

| RELEASE_ALL_LOCKS() |

--+

| 1 |

+-- -+

1 row in set (0.0004 sec)

-- Connection 2

Connection 2> SELECT RELEASE_ALL_LOCKS();

+-

| RELEASE_ALL_LOCKS() |

+- -+

| 2 |

+-- -+

1 row in set (0.0002 sec)

2020-07-20 20:50:46.749971 0 [INFO] Disconnecting for the workload

Listing 6-1

2020-07-20 20:50:46.749971 0 [INFO] Completed the workload Listing 6-1

Available workloads:

# Name

Description

1 Listing 2-1

Example use of the metadata_locks table

2 Listing 2-2

Example of using the table_handles table

3 Listing 2-3

Using the data_locks table

Choose workload (# or name - empty to exit):

mysql-py>

There are a few things to notice from this example. After choosing the workload, you are asked for a password. This is the password for the MySQL account that you are using. The other connection options are taken from the session.uri property in MySQL Shell, but for security reasons, the password is not stored. If you execute multiple workloads in one invocation of run(), you will only be prompted for the password once.

At the start of the execution of the workload, there is an overview of the process list ids (as from SHOW PROCESSLIST), the (Performance Schema) thread ids, and the last event ids before the start of the workload for each connection used for the workload:

<table><tr><td>-- Connection</td><td>Processlist ID</td><td>Thread ID</td><td>Event ID</td></tr><tr><td>--</td><td>1</td><td>105</td><td>249</td></tr><tr><td>--</td><td>2</td><td>106</td><td>250</td></tr></table>

You can use these ids to execute your own investigative queries, and you can use the overview to identify listings that have been implemented as a workload in concurrency_book.generate.run().

At the end of executing the workload, this example has three queries you can execute to investigate the issue the example demonstrates. You can execute one or more of these by specifying the number of the query (one query at a time). In the code listings in this

book, the output of an investigation is preceded with a comment showing which of the investigations has been executed, for example

-- Investigation #2

The number of investigations per workload varies from none to more than ten. The listings in the book do not always include the result of all of the investigations as some are left as inspiration and further examination of the issue.

Once you are done with the investigation, submit an empty answer to exit from the workload. If you do not want to execute more workloads, submit an empty answer again to exit the run() method.

If you only want to execute a single workload, you can specify the name as an argument to run(). This can be particularly useful when executing a workload as a command given directly on the command line when invoking MySQL Shell, for example shell> mysqlsh --user $\bf \tilde { = }$ myuser --py -e "concurrency_book.generate. run('Listing 6-1')"

The remainder of this chapter describes the three schemas used for the examples in this book.

# Test Data: The world Schema

The world sample database is one of the most commonly used databases for simple tests. It consists of three tables with a few hundred to a few thousand rows. This makes it a small data set which means it can easily be used even on small test instances.

# Schema

The database consists of the city, country, and countrylanguage tables. The relationship between the tables is shown in Figure 1-3.

![](images/e2cc77f3e1f663b233784943ba334f5b8447fafc3e623ae1539bbd1bf5402f3d.jpg)  
Figure 1-3. The world database

The country table includes information about 239 countries and serves as the parent table in foreign keys from the city and countrylanguage tables. There is a total of 4079 cities in the database and 984 combinations of country and language.

# Installation

You can download a file with the table definitions and data from https://dev.mysql. com/doc/index-other.html. Oracle provides access to several example databases from that page in the section Example Databases as shown in Figure 1-4.

# Example Databases

<table><tr><td>Title</td><td>Download DB</td><td>HTML Setup Guide</td><td>PDF Setup Guide</td></tr><tr><td>employee data (large dataset, includes data and test/verification suite)</td><td>GitHub</td><td>View</td><td>US Ltr | A4</td></tr><tr><td>world database</td><td>Gzip | Zip</td><td>View</td><td>US Ltr | A4</td></tr><tr><td>world_x database</td><td>TGZ | Zip</td><td>View</td><td>US Ltr | A4</td></tr><tr><td>sakila database</td><td>TGZ | Zip</td><td>View</td><td>US Ltr | A4</td></tr><tr><td>menagerie database</td><td>TGZ | Zip</td><td></td><td></td></tr></table>

Figure 1-4. The table with links to the example databases

The downloaded file consists of a single file named world.sql.gz or world.sql.zip depending on whether you chose the Gzip or ZIP link. In either case, the downloaded archive contains a single file world.sql. The installation of the data is straightforward as all that is required is to execute the script.

You can source the world.sql from either MySQL Shell or the mysql command-line client. From MySQL Shell you use the \source command to load the data:

MySQL [localhost ssl] SQL> \source world.sql

If you use the legacy mysql command-line client, use the SOURCE command instead: mysql> SOURCE world.sql

In either case, add the path to the world.sql file if it is not located in the directory where you started MySQL Shell or mysql.

If you prefer to use a GUI, then you can also load the world database using MySQL Workbench. While connected to the MySQL instance you want to load the world schema into, you click on File in the menu followed by Run SQL Script as shown in Figure 1-5.

![](images/5e59be821e0607e36af86ea3e09e531590c0a261afaf7333c8ab411c12da1958.jpg)  
Figure 1-5. Running an SQL script from MySQL Workbench

This opens a file explorer where you can browse for the file. Navigate to the directory where you have saved the uncompressed world.sql file and choose it. The result is the dialog shown in Figure 1-6 where you can review the first part of the script and optionally set the default schema name and character set.

![](images/d9ad46a10b7f9b6ad7f54d8cf3bee8a5909da762c1387ad0cfabe6d9388c918d.jpg)  
Figure 1-6. The dialog in MySQL Workbench for reviewing the script

In the case of the world schema, both the schema name and character set are included in the script, so there is no need to (and no effect of) setting those settings. Click on Run to execute the script. A dialog shows the progress information while MySQL executes the script. When the operation has completed, close the dialog. Optionally, you can refresh the list of schemas in the sidebar by clicking on the two arrows chasing each other as shown in Figure 1-7.

![](images/4a7c45c87331f4ec283567fbec7b960defe8ac20fa40af61c7e3099025ecbe67.jpg)  
Figure 1-7. Refresh the list of schemas by clicking on the two arrows

While the world schema is great for much testing because of its simplicity and small size, that also limits its usefulness, and sometimes a bit more complex database is required.

# Test Data: The sakila Schema

The sakila database is a realistic database that contains a schema for a film rental business with information about the films, inventory, stores, staff, and customers. It adds a full text index, a spatial index, views, and stored programs to provide a more complete example of using MySQL features. The database size is still very moderate, making it suitable for small instances.

# Schema

The sakila database consists of 16 tables, seven views, three stored procedures, three stored functions, and six triggers. The tables can be split into three groups, customer data, business, and inventory. For brevity, not all columns are included in the diagrams, and most indexes are not shown. Figure 1-8 shows a complete overview of the tables, views, and stored routines.

![](images/9bca8bbe557842c42a0023a5379569843835caa70cf1c2402c87afc4877eeb02.jpg)  
Figure 1-8. Overview of the sakila database

The tables with customer-related data (plus addresses for staff and stores) are in the area in the top-left corner. The area in the lower left includes data related to the business, and the area in the top right contains information about the films and inventory. The lower right is used for the views and stored routines.

Tip You can view the entire diagram (though formatted differently) by opening the sakila.mwb file included with the installation in MySQL Workbench. This is also a good example of how you can use enhanced entity-relationship (EER ) diagrams in MySQL Workbench to document your schema.

As there is a relatively large number of objects, they will be split into five groups (each of the table groups, views, and stored routines) when discussing the schema. The first group is the customer-related data with the tables shown in Figure 1-9.

![](images/fcc505318bb33a4714158daa25944c00abe283aa38498c97af67f4803fea5a6a.jpg)  
Figure 1-9. The tables with customer data in the sakila database

There are four tables with data related to the customers. The customer table is the main table, and the address information is stored in the address, city, and country tables.

There are foreign keys between the customer and business groups with a foreign key from the customer table to the store table in the business group. There are also four foreign keys from tables in the business group to the address and customer tables. The business group is shown in Figure 1-10.

![](images/063b93965b5a320c0040f8a061c9ebf0eee1ca2dfd783fdf3f05de40d03523b4.jpg)  
Figure 1-10. The tables with business data in the sakila database

The business tables contain information about the stores, staff, rentals, and payments. The store and staff tables have foreign keys in both directions with staff belonging to a store and a store having a manager that is part of the staff. Rentals and payments are handled by a staff member and thus indirectly linked to a store, and payments are for a rental.

The business group of tables is the one with the most relations to other groups. The staff and store tables have foreign keys to the address table, and the rental and payment tables reference the customer. Finally, the rental table has a foreign key to the inventory table which is in the inventory group. The diagram for the inventory group is shown in Figure 1-11.

![](images/f088466dcdd8fbc22c88b96b03ba2d940fac29cc38121b4b033113eb8b0acb31.jpg)  
Figure 1-11. The tables with inventory data in the sakila database

The main table in the inventory group is the film table which contains the metadata about the films the stores offer. Additionally, there is the film_text table with the title and description with a full text index.

There is a many-to-many relationship between the film and the category and actor tables. Finally, there is a foreign key from the inventory table to the store table in the business group.

That covers all the tables in the sakila database, but there are also some views as shown in Figure 1-12.

![](images/cc85665c46d20ffd89fec007baa14eb36c2059ba4b9bd26437a4c74417de74fb.jpg)  
Figure 1-12. The views in the sakila database

The views can be used like reports and can be divided into two categories. The film_list, nicer_but_slower_film_list, and actor_info views are related to the films stored in the database. The second category contains information related to the stores in the sales_by_store, sales_by_film_category, staff_list, and customer_list views.

To complete the database, there are also the stored functions and procedures shown in Figure 1-13.

![](images/c60f53b458e97f933846338812581b4f74df43002438af10a2ac275955fdc8e7.jpg)  
Figure 1-13. The stored routines in the sakila database

The film_in_stock() and film_not_in_stock() procedures return a result set consisting of the inventory ids for a given film and store based on whether the film is in stock or not. The total number of inventory entries found is returned as an out parameter. The rewards_report() procedure generates a report based on minimum spends for the last month.

The get_customer_balance() function returns the balance for a given customer on a given date. The two remaining functions check the status of an inventory id with inventory_held_by_customer() returning customer id of the customer currently renting that item (and NULL if no customer is renting it), and if you want to check whether a given inventory id is in stock, you can use the inventory_in_stock() function.

# Installation

You can download a file with the installation scripts to install the sakila schema from https://dev.mysql.com/doc/index-other.html like for the world database.

The downloaded file expands into a directory with three files of which two create the schema and data and the last file contains the ETL diagram in the format used by MySQL Workbench.

Note T he sakila database is also available with the download of the employees database; however, this section and the examples later in the book use the copy of the sakila database that is downloaded from MySQL’s homepage.

# The files are

sakila-data.sql: The INSERT statements needed to populate the tables as well as the trigger definitions.   
• sakila-schema.sql: The schema definition statements.   
sakila.mwb: The MySQL Workbench ETL diagram. This is similar to that shown in Figure 1-7 with details in Figures 1-8 to 1-12.

You install the sakila database by first sourcing the sakila-schema.sql file and then the sakila-data.sql file. For example, the following is using MySQL Shell:

MySQL [localhost+ ssl] SQL> \source sakila-schema.sql MySQL [localhost+ ssl] SQL> \source sakila-data.sql

Add the path to the files if they are not located in the current directory.

# Test Data: The employees Schema

The employees database (called employee data on the MySQL documentation download page; the name of the GitHub repository is test_db) was originally created by Fusheng Wang and Carlo Zaniolo and is the largest of the test data sets linked from MySQL’s homepage. It comes with a choice of using nonpartitioned tables or partitioning two of the largest tables. The total size of the data files is around 180 MiB for the nonpartitioned version and 440 MiB for the partitioned version.

# Schema

The employees database consists of six tables and two views. You can optionally install two more views, five stored functions, and two stored procedures. The tables are shown in Figure 1-14.

![](images/cf6762b8c6106f1176d99f7d77d6e79f2502a2b6b60d2330562de1cb10b966b0.jpg)  
Figure 1-14. The tables, views, and routines in the employees database

By today’s standards, it is still a relatively small amount of data in the database, but it is big enough that you can start to see lower-level contention, and for this reason, it is the schema used to cause semaphore waits in Chapter 18.

# Installation

You can download a ZIP file with the files required for the installation, or you can clone the GitHub repository at https://github.com/datacharmer/test_db. At the time of writing, there is only a single branch named master. If you have downloaded the ZIP file, it will unzip into a directory named test_db-master.

There are several files. The two relevant for installing the employees database in MySQL 8 are employees.sql and employees_partitioned.sql. The difference is whether the salaries and titles tables are partitioned. This book uses the unpartitioned schema. (There is also employees_partitioned_5.1.sql which is meant for MySQL 5.1 where the partitioning scheme used in employees_partitioned.sql is not supported.)

The data is loaded by sourcing the .dump files using the SOURCE command which is only supported in MySQL Shell in 8.0.19 (in practice 8.0.20 due to a bug) and later. Go to the directory with the source files, and choose the employees.sql or employees_ partitioned.sql file, depending on whether you want to use partitioning or not, for example

mysql> \source employees.sql

The import takes a little time and completes by showing how long it took:

```txt
+  
| data_load_time_diff |  
+  
| 00:02:50 |  
+  
1 row in set (0.0085 sec 
```

Optionally, you can load some extra views and stored routines by sourcing the objects.sql file:

```txt
mysql> \source objects.sql 
```

When you load the employees schema using the concurrency_book.generate. load() method, the objects.sql file is always included.

You are now ready to dive into the world of MySQL concurrency.

# Summary

This chapter started the journey to understand MySQL concurrency of which locks and transactions are important topics. First it was discussed why locks are needed and at what levels they exist. Then it was covered that transactions must be included in the discussion as some locks are held for the duration of the transaction and the transaction isolation level influences the duration of the locks as well as the number of locks.

The rest of the chapter discussed how the examples are used in this book and introduced the three sets of test data that is required to reproduce the test cases. To make it easier to load the data and execute the test cases, the concurrency_book.generate module for MySQL Shell was also introduced.

In the next chapter, it will be discussed how you can monitor locks.

# Monitoring Locks and Mutexes

Monitoring is essential to understanding where bottlenecks occur in your system. You need to use monitoring both to determine sources of contention and to verify that the changes you make reduce the contention.

This and the two following two chapters provide an overview of lock and mutex monitoring, InnoDB transaction monitoring, and general transaction monitoring in the Performance Schema. The remainder of the books shows examples of how you can use these monitoring resources to identify and investigate contention. Particularly Chapters 13–18 use monitoring extensively during the discussion of the case studies.

In this chapter, you will learn how you can monitor locks and mutexes. The primary resource is the Performance Schema which is covered first. Next, the ready-made reports in the sys schema are discussed. The second half of the chapter covers the status metrics, InnoDB lock monitoring, and InnoDB mutex monitoring.

Note Do not worry if you do not know what the various locks and mutexes are yet. You will learn this later with examples of using the monitoring sources discussed in this chapter.

# The Performance Schema

The Performance Schema contains the source of most of the lock information available except for deadlocks. Not only can you use the lock information in the Performance Schema directly; it is also used for two lock-related views in the sys schema. Additionally, you can use the Performance Schema to investigate the low-level synchronization objects such as mutexes. First, it will be shown how metadata and table locks can be investigated.

# Metadata and Table Locks

Metadata locks are the most generic of the higher-level locks, and there is support for a wide range of locks ranging from the global read lock to low-level locks like for the access control list (ACL). The locks are monitored using the metadata_locks table which contains information about user-level locks, metadata locks, and similar. To record information, the wait/lock/metadata/sql/mdl Performance Schema instrument must be enabled (it is enabled by default in MySQL 8). There is an example later showing how you can enable instruments.

The metadata_locks table contains 11 columns which are summarized in Table 2-1.

Table 2-1. The performance_schema.metadata_locks table   

<table><tr><td>Column Name</td><td>Description</td></tr><tr><td>OBJECT_TYPE</td><td>The kind of lock that is held such as GLOBAL for the global read lock and TABLE for tables and views. 
Appendix A includes a complete list of possible values.</td></tr><tr><td>OBJECT_SCHEMA</td><td>The schema the object that is locked belongs to.</td></tr><tr><td>OBJECT_NAME</td><td>The name of the locked object.</td></tr><tr><td>COLUMN_NAME</td><td>For column level locks, the column name of the locked column.</td></tr><tr><td>OBJECT_INSTANCE_BEGIN</td><td>The memory address of the object.</td></tr><tr><td>LOCK_TYPE</td><td>The lock access level such as shared, exclusive, or intention. 
Appendix A includes a complete list of possible values.</td></tr><tr><td>LOCK_duration</td><td>How long the lock is held for. Supported values are STATEMENT, TRANSACTION, and EXPLICIT.</td></tr><tr><td>LOCK_STATUS</td><td>The status of the lock. In addition to a granted and pending status, it can also show that the lock request timed out, was a victim, etc.</td></tr><tr><td>SOURCE</td><td>The place in the source code where the lock was requested.</td></tr><tr><td>OWNER_THREAD_ID</td><td>The Performance Schema thread id of the thread that requested the lock.</td></tr><tr><td>OWNER_EVENT_ID</td><td>The event id of the event requesting the lock.</td></tr></table>

The primary key of the table is the OBJECT_INSTANCE_BEGIN column.

Listing 2-1 shows an example of obtaining a table metadata lock and querying it in the metadata_locks table. Some of the details will be different for you.

Listing 2-1. Example use of the metadata_locks table   
```sql
-- Connection Processlist ID Thread ID Event ID   
-- 1 19 59 6   
-- Connection 1   
mysql> START TRANSACTION;   
Query OK, 0 rows affected (0.0003 sec)   
mysql> SELECT \* FROM world.city WHERE ID = 130; ++ ++ ++ ++ ++ ++ ++ ++ ++ ++ ++ ++ ++ ++ ++ ++ ++ ++ ++ ++ ++ ++ ++ ++ ++ ++ ++ ++ ++ ++ ++ ++ ++ ++ ++ ++ ++ ++ ++ ++ ++ ++ ++ ++ ++ ++ ++ ++ ++ ++ +  
| ID | Name | CountryCode | District | Population |  
+---  
| 130 | Sydney | AUS | New South Wales | 3276207 |  
+---  
1 row in set (0.0005 sec)   
mysql> SELECT * FROM performance_schema_metadata_locks WHERE OBJECT_TYPE = 'TABLE' AND OBJECT_SCHEMA = 'world' AND OBJECT_NAME = 'city' AND OWNER_THREAD_ID = PS_CURRENT_THREAD_ID()\G  
********** 1. row ***  
OBJECT_TYPE: TABLE  
OBJECT_SCHEMA: world  
OBJECT_NAME: city  
COLUMN_NAME: NULL  
OBJECT_INSTANCE_BEGIN: 2639965404080  
LOCK_TYPE: SHARED_READ  
LOCK_duration: TRANSACTION  
LOCK_STATUS: GRANTED  
SOURCE: sql_scan.cc:6162 
```

Chapt er 2 Monit oring Locks and Mut exes   
```txt
OWNER_THREAD_ID: 59  
OWNER_EVENT_ID: 10  
1 row in set (0.0006 sec)  
mysql> ROLLBACK;  
Query OK, 0 rows affected (0.0006 sec) 
```

Here you can see that it is a table level lock on the world.city table. It is a shared read lock, so other connections can obtain the same lock concurrently.

If you want to find out why a connection is waiting for its lock request to be granted, you need to query the metadata_locks table for a row where the OBJECT_TYPE, OBJECT_ SCHEMA, and OBJECT_NAME are the same as for the pending lock and the LOCK_STATUS is GRANTED. That is, to find all cases of pending locks and what is blocking them, you need a query that self-joins the table:

```sql
SELECT OBJECT_TYPE, OBJECT_SCHEMA, OBJECT_NAME, w.OWNER_THREAD_ID AS WAITING_THREAD_ID, b.OWNER_THREAD_ID AS BLOCKING_THREAD_ID FROM performance_schema_metadata_locks w INNER JOIN performance_schema_metadata_locks b USING (OBJECT_TYPE, OBJECT_SCHEMA, OBJECT_NAME) WHERE w.LOCK_STATUS = 'PENDING' AND b.LOCK_STATUS = 'GRANTED'; 
```

You can optionally join on other Performance Schema tables such as events_ statements_current to get more information about the connections involved in the lock wait. Alternatively, as it will be discussed later, for table metadata locks, you can use the sys.schema_table_lock_waits view.

A less frequently used table is table_handles which holds information about the open table handles including which table locks are currently locked. The wait/ lock/table/sql/handler Performance Schema instrument must be enabled for data to be recorded (this is the default). The information available is similar to that of the metadata_locks table, and Listing 2-2 shows an example of an explicit read lock on the world.city table. Some of the details will be different for you.

Listing 2-2. Example of using the table_handles table   
-- Connection Processlist ID Thread ID Event ID   
-- 1 21 61 6   
-- Connection 1   
mysql> LOCK TABLE world.city READ;   
Query OK, O rows affected (0.0004 sec)   
mysql> SELECT \* FROM performance_schema.tablehandles WHERE OBJECT_SCHEMA $=$ 'world' AND OBJECT_NAME $=$ 'city' AND OWNER_THREAD_ID $=$ PS_CURRENT_THREAD_ID()\G   
********** 1. row   
OBJECT_TYPE: TABLE OBJECT_SCHEMA: world OBJECT_NAME: city   
OBJECT instances BEGIN: 2639971828776 OWNER_THREAD_ID: 61 OWNER_EVENT_ID: 8 INTERNAL_LOCK: NULL EXTERNAL_LOCK: READ EXTERNAL   
1 row in set (0.0013 sec)   
mysql> UNLOCK TABLES;   
Query OK, O rows affected (0.0004 sec)

The INTERNAL_LOCK column contains lock information at the SQL level such as explicit table locks on non-InnoDB tables, while the EXTERNAL_LOCK contains lock information at the storage engine level including explicit table locks for all tables.

Unlike the metadata_locks table, you cannot use the table_handles table to investigate lock contentions (but the metadata_locks table also includes explicit table locks like in this example, so you can use that).

The metadata_locks and table_handles tables concern the highest-level locks. The next step on the lock granularity latter is data locks which have their own tables.

# Data Locks

Data locks are at the medium level between the metadata locks and the synchronization objects. What makes data locks special is that you have a large variety of lock types such as record locks, gap locks, insert intention locks, etc. that interact in complex ways as described in Chapter 7. This makes the monitoring tables for data locks particularly useful.

The data lock information is split into two tables:

data_locks: This table contains details of table and records locks at the InnoDB level. It shows all locks currently held or are pending.   
• data_lock_waits: Like the data_locks table, it shows locks related to InnoDB, but only those waiting to be granted with information on which thread is blocking the request.

You will often use these in combination to find information about lock waits.

MySQL 8 has seen a change in the way that the lock monitoring tables work. In MySQL 5.7 and earlier, the information was available in two InnoDB-specific views in the Information Schema, INNODB_LOCKS and INNODB_LOCK_WAITS. The major differences are that the Performance Schema tables are created to be storage engine agnostic and information about all locks are always made available, whereas in MySQL 5.7 and earlier, only information about locks involved in lock waits were exposed. That all locks are always available for investigation makes the MySQL 8 tables much more useful to learn about locks.

The data_locks table is the main table with detailed information about each lock. The table has 15 columns as described in Table 2-2.

Table 2-2. The performance_schema.data_locks table   

<table><tr><td>Column Name</td><td>Description</td></tr><tr><td>ENGINE</td><td>The storage engine for the data. For MySQL Server, this will always be InnoDB.</td></tr><tr><td>ENGINE_LOCK_ID</td><td>The internal id of the lock as used by the storage engine. You should not rely on the id having a particular format.</td></tr><tr><td>ENGINE_transaction_ID</td><td>The transaction id specific to the storage engine. For InnoDB, you can use this id to join on thetrx_id column in the information_schema.INNODB_TRX view. You should not rely on the id having a particular format, and the id may change in the duration of a transaction.</td></tr></table>

(continued)

Table 2-2. (continued)   

<table><tr><td>Column Name</td><td>Description</td></tr><tr><td>THREAD_ID</td><td>The Performance Schema thread id of the thread that made the lock request.</td></tr><tr><td>EVENT_ID</td><td>The Performance Schema event id of the event that made the lock request. You can use this id to join with several of the events % tables to find more information on what triggered the lock request.</td></tr><tr><td>OBJECT_SCHEMA</td><td>The schema the object that is subject of the lock request is in.</td></tr><tr><td>OBJECT_NAME</td><td>The name of the object that is subject of the lock request.</td></tr><tr><td>PARTITION_NAME</td><td>For locks involving partitions, the name of the partition.</td></tr><tr><td>SUBPARTITION_NAME</td><td>For locks involving subpartitions, the name of the subpartition.</td></tr><tr><td>INDEX_NAME</td><td>For locks involving indexes, the name of the index. Since everything is an index for InnoDB, the index name is always set for record level locks on InnoDB tables. If the row is locked, the value will be PRIMARY or GEN_CLUST_INDEX depending on whether you have an explicit primary key or the table used a hidden clustered index.</td></tr><tr><td>OBJECT instances BEGIN</td><td>The memory address of the lock request.</td></tr><tr><td>LOCK_TYPE</td><td>The level of the lock request. For InnoDB, the possible values are TABLE and RECORD.</td></tr><tr><td>LOCK_MODE</td><td>The locking mode used. This includes whether it is a shared or exclusive lock and the finer details of the lock, for example, REC_NOT_GAP for a record lock but no gap lock.</td></tr><tr><td>LOCK_STATUS</td><td>Whether the lock is pending (WAITING) or has been granted (GRANTED).</td></tr><tr><td>LOCK_DATA</td><td>Information about the data that is locked. This can, for example, be the index value of the locked index record.</td></tr></table>

The primary key of the table is (ENGINE_LOCK_ID, ENGINE).

An example of acquiring two locks and querying the data_locks table is shown in Listing 2-3. Information such as the ids and the memory address will differ for you.

Listing 2-3. Using the data_locks table   
```sql
-- Connection Processlist ID Thread ID Event ID
-- 1 23 64 6
-- Connection 1
mysql> START TRANSACTION;
Query OK, 0 rows affected (0.0003 sec)
mysql> SELECT *
FROM world.city
WHERE ID = 130
FOR SHARE;
+
| ID | Name | CountryCode | District | Popul
+
| 130 | Sydney | AUS | New South Wales | 3
+
| row in set (0.0068 sec)
mysql> SELECT *
FROM performance_schema.data_locks
WHERE THREAD_ID = PS_CURRENT_THREAD_ID()\G
*****
ENGINE: INNODB
ENGINE_LOCK_ID: 2639727636640:3165:26396907121
ENGINE_TRANSACTION_ID: 284114704347296
 THREAD_ID: 64
EVENT_ID: 10
OBJECT_SCHEMA: world
OBJECT_NAME: city
PARTITION_NAME: NULL
SUBPARTITION_NAME: NULL
INDEX_NAME: NULL
OBJECT instances BEGIN: 2639690712184
LOCK_TYPE: TABLE 
```

```txt
LOCK_MODE: IS  
LOCK_STATUS: GRANTED  
LOCK_DATA: NULL  
********** 2. row ***  
ENGINE: INNODB  
ENGINE_LOCK_ID: 2639727636640:1926:6:131:2639690709400  
ENGINETRACTION_ID: 284114704347296  
THREAD_ID: 64  
EVENT_ID: 10  
OBJECT_SCHEMA: world  
OBJECT_NAME: city  
PARTITION_NAME: NULL  
SUBPARTITION_NAME: NULL  
INDEX_NAME: PRIMARY  
OBJECT instances BEGIN: 2639690709400  
LOCK_TYPE: RECORD  
LOCK_MODE: S,REC_NOT_GAP  
LOCK_STATUS: GRANTED  
LOCK_DATA: 130  
2 rows in set (0.0018 sec)  
mysql> ROLLBACK;  
Query OK, o rows affected (0.0007 sec) 
```

In this example, the query obtains an insert intention (IS) lock on the world.city table and a shared (S) record, but not gap, lock (REC NOT_GAP) on the primary key with the value 130.

The data_lock_waits table is simpler as it just includes the basic information about current cases of lock contention as shown in Table 2-3.

Table 2-3. The performance_schema.data_lock_waits table   

<table><tr><td>Column Name</td><td>Description</td></tr><tr><td>ENGINE</td><td>The storage engine where the lock contention occurs.</td></tr><tr><td>REQUESTINGENGINE_LOCK_ID</td><td>The ENGINE_LOCK_ID for the pending lock.</td></tr><tr><td>REQUESTINGENGINE_TRANSACTION_ID</td><td>The ENGINE_TRANSACTION_ID for the pending lock.</td></tr><tr><td>REQUESTING_THREAD_ID</td><td>The THREAD_ID for the pending lock.</td></tr><tr><td>REQUESTING_EVENT_ID</td><td>The EVENT_ID for the pending lock.</td></tr><tr><td>REQUESTING_OBJECTINSTANCE_BEGIN</td><td>The OBJECTINSTANCE_BEGIN for the pending lock.</td></tr><tr><td>BLOCKINGENGINE_LOCK_ID</td><td>The ENGINE_LOCK_ID for the blocking lock.</td></tr><tr><td>BLOCKINGENGINE_TRANSACTION_ID</td><td>The ENGINE_TRANSACTION_ID for the blocking lock.</td></tr><tr><td>BLOCKING_THREAD_ID</td><td>The THREAD_ID for the blocking lock.</td></tr><tr><td>BLOCKING_EVENT_ID</td><td>The EVENT_ID for the blocking lock.</td></tr><tr><td>BLOCKING_OBJECTINSTANCE_BEGIN</td><td>The OBJECTINSTANCE_BEGIN for the blocking lock.</td></tr></table>

The table does not have a primary key. The primary purpose of the table is to provide a simple way to determine the pending and blocking lock requests involved in lock contention. You can then join on the data_locks table using the REQUESTING ENGINE_TRANSACTION_ID and BLOCKING_ENGINE_TRANSACTION_ID columns as well as to other tables to obtain more information. A good example of this is the sys.innodb_lock_ waits view.

This far, the Performance Schema tables that have been discussed have been for locks that are directly a result of the statements that are executed. There are also lower-level synchronization waits that are important to monitor in high-concurrency situations.

# Synchronization Waits

The synchronization waits are the most difficult to monitor for several reasons. They occur very frequently, usually at a very short duration, and monitoring them has a high overhead. Instrumentation of the synchronization waits is also not enabled by default.

The synchronization waits are split into five categories:

• cond: Conditions used in thread to thread signals.   
• mutex: A mutual exclusion point that protects code parts or other resources.   
prlock: A priority read/write lock.   
• rwlock: A read/write lock used to limit concurrent access to specific variables, for example, for changing the gtid_mode system variable.   
sxlock: A shared-exclusive read/write lock. This is currently only used by InnoDB, for example, to improve the scalability of the B-tree searches.

The instrument names for the synchronization waits start with wait/synch/ followed by the name of the category, the area the wait belongs to (such as sql or innodb), and the name of the wait. For example, the mutex guarding the InnoDB double write buffer has the name wait/synch/mutex/innodb/dblwr_mutex.

You enable the instrumentation of the synchronization waits by setting the ENABLED and optionally the TIMED columns in the performance_schema.setup_instruments table for the instruments you want to monitor. Additionally, you will need to enable events_ waits_current and optionally events_waits_history and/or events_waits_history_ long in performance_schema.setup_consumers. For example, to monitor the mutex on the InnoDB double write buffer

mysql> UPDATE performance_schemasetup_instruments SET ENABLED $=$ 'YES', TIMED $=$ 'YES' WHERE NAME $=$ 'wait/synch/mutex/innodb/dbldr_mutex'; Query OK, 1 row affected (0.0011 sec)   
Rows matched: 1 Changed: 1Warnings: 0   
mysql> UPDATE performance_schemasetup_consumers SET ENABLED $=$ 'YES' WHERE NAME $=$ 'events_waits_current'; Query OK, 1 row affected (0.0005 sec)   
Rows matched: 1 Changed: 1Warnings: 0

In general, it is better to enable monitoring of synchronization instruments in the configuration file to ensure they are properly set up from the time MySQL starts up:

```json
[mysqld] 
```

```txt
performance_schema_instrument = wait/synch/mutex/innodb/dbldr_mutex=ON  
performance_schema_consumer_events_waits_current = ON 
```

Then restart MySQL.

Caution Be very careful in enabling instrumentation of the synchronization waits and the corresponding consumers on production systems. Doing so can cause a high enough overhead that you will effectively have an outage. The more that are enabled, the higher overhead and the more likely the monitoring interferes with the measurements, so the conclusions are wrong.

You can now monitor the waits using one of the events_waits_% tables:

events_waits_current: The current ongoing or last completed wait events for each existing thread. This requires the events_waits_ current consumer to be enabled.   
events_waits_history: The last ten (the performance_schema_ events_waits_history_size option) wait events for each existing thread. This requires the events_waits_history consumer to be enabled in addition to the events_waits_current consumer.   
events_waits_history_long: The last 10,000 (the performance_ schema_events_waits_history_long_size option) events globally, including for threads that no longer exist. This requires the events_ waits_history_long consumer to be enabled in addition to the events_waits_current consumer.   
events_waits_summary_by_account_by_event_name: The wait events grouped by the username and hostname of the accounts (also called actors in the Performance Schema).   
events_waits_summary_by_host_by_event_name: The wait events grouped by the hostname of the account triggering the event and event name.

events_waits_summary_by_instance: The wait events grouped by the event name as well as the memory address (OBJECT_INSTANCE_ BEGIN) of the object. This is useful for events with more than one instance to monitor whether the waits are evenly distributed among the instances. An example is the table cache mutex (wait/synch/ mutex/sql/LOCK_table_cache) which has one object per table cache instance (table_open_cache_instances).   
events_waits_summary_by_thread_by_event_name: The wait events for currently existing threads grouped by the thread id and event name.   
events_waits_summary_by_user_by_event_name: The wait events grouped by the username of the account triggering the event and event name.   
events_waits_summary_global_by_event_name: The wait events grouped by the event names. This table is useful to get an overview of how much time is spent waiting for a given type of event.

Given how short lived a synchronization wait normally is and how frequently they are encountered, the summary tables are usually the most useful for investigating waits using the Performance Schema. That said, since the relevant wait instruments are not enabled by default and they have relatively high overhead when monitoring them, usually the semaphore section of the InnoDB monitor or the SHOW ENGINE INNODB MUTEX statement as described later in this chapter is used for InnoDB mutexes and semaphores. The exception is when you want to investigate a specific contention issue.

Another useful way to use the Performance Schema for lock analysis is to query for the errors that statements are encountering.

# Statement and Error Tables

The Performance Schema includes several tables that can be used to investigate the errors that are encountered. Since a failure to obtain a lock either due to a timeout or a deadlock triggers an error, you can query for lock-related errors to determine which statements, accounts, and so on that are most affected by lock contention.

At the individual statement level, you can use the events_statements_current, events_statements_history, and events_statements_history_long to see whether any error or a specific error has occurred. The first two of the tables are enabled by default, whereas the events_statements_history_long table requires that you enable the events_statements_history_long consumer. Listing 2-4 shows an example of a lock wait timeout and how it shows up in the events_statements_history table.

Listing 2-4. Example of a lock error in the statement tables   
-- Connection 1 Connection 1> START TRANSACTION; Query OK, 0 rows affected (0.0003 sec)   
```diff
-- Connection Processlist ID Thread ID Event ID
-- 1 63 179 6
-- 2 64 180 6 
```

```txt
Connection 1> UPDATE world.city  
SET Population = Population + 1  
WHERE ID = 130;  
Query OK, 1 row affected (0.0011 sec) 
```

Rows matched: 1 Changed: 1 Warnings: 0   
```sql
-- Connection 2  
Connection 2> SET SESSION innodb_lock_wait_timeout = 1;  
Query OK, 0 rows affected (0.0003 sec) 
```

Connection 2> START TRANSACTION; Query OK, 0 rows affected (0.0002 sec)   
```sql
Connection 2> UPDATE world.city  
SET Population = Population + 1  
WHERE ID = 130; 
```

ERROR: 1205: Lock wait timeout exceeded; try restarting transaction

```sql
Connection 2> SELECT thread_id, event_id, FORMAT_PICO_TIME lock_time) AS lock_time, sys.format_statement (SQL_text) AS statement, digest, mysql_errno, returned sqlstate, message_text, errors FROM performance_schema.events statements_history WHERE thread_id = PS_CURRENT_THREAD_ID() AND mysql_errno > 0\G   
********** 1. row   
thread_id: 180 event_id: 10 lock_time: 271.00 us statement: UPDATE world.city SET Popul ... Population + 1 WHERE ID = 130 digest: 3e9795ad6fc0f4e3a4b4e99f33fbab2dc7b40d0761a8adbc60abfab 02326108d mysql_errno: 1205 returned sqlstate: HY000 message_text: Lock wait timeout exceeded; try restarting transaction errors: 1   
1 row in set (0.0016 sec)   
-- Connection 1   
Connection 1> ROLLBACK;   
Query OK, 0 rows affected (0.0472 sec)   
-- Connection 2   
Connection 2> ROLLBACK;   
Query OK, 0 rows affected (0.0003 sec) 
```

There are a few things worth noting from the example. The first thing is that the lock time is only 271 microseconds despite that it took a full second before the lock wait timeout occurred. That is, waiting for a record lock inside InnoDB is not adding to the lock time reported by the Performance Schema, so you cannot use that to investigate record level lock contention.

The second thing is that the mysql_errno, returned_sqlstate, and message_text include the same error information as it returned to the client which makes it useful for querying as it is also done in this case. Third, the errors column contains a count of the number of errors encountered. While the count doesn’t say anything about the nature of the error, it is useful as unlike the columns with the specifics of the error, the error counter is also present in the statement summary tables, so you can use it to find which statements encounter an error of any kind.

Tip It can be useful to log encountered errors in the application. You can then, for example, analyze the application logs with a service like Splunk to generate reports showing which errors are encountered and when they are a problem.

A group of summary tables of special interest in this context consists of the tables that summarize errors. There are five such tables grouped by the account, host, thread, user, and global, respectively:

mysql> SHOW TABLES FROM performance_schema LIKE '%error%';

```diff
+ Tables_in_performance_schema (%error%)  
+  
| events Errors_summery_by_account_by_error |  
| events errors_summery_by_host_by_error |  
| events errors_summery_by_thread_by_error |  
| events errors_summery_by_user_by_error |  
| events errors_summery_global_by_error |  
+  
5 rows in set (0.0012 sec) 
```

For example, to retrieve the statistics for lock wait timeouts and deadlocks mysql> SELECT * FROM performance_schema.events_errors_summary_global_by_error WHERE error_name IN ('ER_LOCK_WAIT_TIMEOUT', 'ER_LOCK_DEADLOCK')\G

*************************** 1. row ***************************

```txt
ERROR_NUMBER: 1205  
ERROR_NAME: ER_LOCK_WAIT_TIMEOUT  
SQL_STATE: HY000  
SUM_ERROR_RAISED: 4  
SUM_ERROR_HANDLED: 0  
FIRST_SEEN: 2020-06-28 11:33:10  
LAST_SEEN: 2020-06-28 11:49:30  
********** 2. row ***  
ERROR_NUMBER: 1213  
ERROR_NAME: ER_LOCKDeadLOCK  
SQL_STATE: 40001  
SUM_ERROR_RAISED: 3  
SUM_ERROR_HANDLED: 0  
FIRST_SEEN: 2020-06-27 12:06:38  
LAST_SEEN: 2020-06-27 12:54:27  
2 rows in set (0.0048 sec) 
```

While this does not help you identify which statements encounter the errors, it can help you monitor the frequency you encounter the errors and, in that way, determine whether lock errors become more frequent.

Tip The events_errors_summary_global_by_error is populated with all known errors from the time MySQL is started even if the error has not yet been encountered. So, you can safely query for specific errors at all time including using the table to look up the error number from the name.

The data in the Performance Schema tables is the raw data, either as individual events or aggregated. Often when you investigate lock issues or monitor for lock issues, it is more interesting to determine if there are any lock waits or to obtain a report of the wait events where most time is spent. For that information, you need to use the sys schema.

# The sys Schema

The sys schema can be considered a collection of views that serve as reports on the Performance Schema and Information Schema as well as various utility functions and procedures. For this discussion the focus is on the two views that take the information in the Performance Schema tables and return the lock pairs where one lock cannot be granted because of the other lock. Thus, they show where there are problems with lock waits. The two views are innodb_lock_waits and schema_table_lock_waits.

The innodb_lock_waits view uses the data_locks and data_lock_waits view in the Performance Schema to return all cases of lock waits for InnoDB record locks. It shows information such as what lock the connection is trying to obtain and which connections and queries are involved. The view also exists as x$innodb_lock_waits, if you need the information without formatting.

The schema_table_lock_waits view works in a similar way but uses the metadata_ locks table to return lock waits related to schema objects. The information is also available unformatted in the $\times \$ 5$ chema_table_lock_waits view.

Tip Several views also exist where $\times \$ 1$ is prepended to the view name. This view contains the same information as the view without $\times \$ 1$ in the name except all the data is unformatted. This makes the data more suitable for scripts and programs that process the information.

Chapters 13–17 include examples of using both views to investigate lock issues.

For a high-level view of the contention, you can also use the status counters and InnoDB metrics.

# Status Counters and InnoDB Metrics

There are several status counters and InnoDB metrics that provide information about locking. These are mostly used at the global (instance) level and can be useful to detect an overall increase in lock issues.

# Querying the Data

There are two sources for status counters and InnoDB metrics. The global status counters can be found in the performance_schema.global_status table or with the SHOW GLOBAL STATUS statement. The InnoDB metrics are found in the information_ schema.INNODB_METRICS view.

The InnoDB metrics are similar to the global status variables and can provide some valuable information on status of InnoDB. The NAME column can be used to query the metric by name. At the time of writing, there are 313 visible metrics of which 74 are enabled by default. There is also one hidden metric which is the latch metric that controls whether mutex wait statistics are collected. The metrics are grouped into subsystems (the SUBSYSTEM column), and for each metric there is a description of what the metric measures in the COMMENT column, and the type of metric (counter, value, etc.) can be seen in the TYPE column.

A great way to monitor all of these metrics together is to use the sys.metrics view. Listing 2-5 shows an example of retrieving the metrics.

# Listing 2-5. Lock metrics

```sql
-- Connection Processlist ID Thread ID Event ID  
-- 1 27 69 6  
-- Connection 1  
mysql> SELECT Variable_name,  
Variable_value AS Value,  
Enabled  
FROM sys.metrics  
WHERE Variable_name LIKE 'innodb_row_lock%'  
OR Variable_name LIKE 'Table_locks%'  
OR Variable_name LIKE 'innodb_rwlock%'  
OR Type = 'InnoDB Metrics - lock'; 
```

<table><tr><td>Variable_name</td><td>Value</td><td>Enabled</td></tr><tr><td>innodb_row_lock_current_waits</td><td>0</td><td>YES</td></tr><tr><td>innodb_row_lock_time</td><td>2163</td><td>YES</td></tr><tr><td>innodb_row_lock_time_avg</td><td>721</td><td>YES</td></tr><tr><td>innodb_row_lock_time_max</td><td>2000</td><td>YES</td></tr><tr><td>innodb_row_lock_waits</td><td>3</td><td>YES</td></tr><tr><td>table_locks_immediate</td><td>330</td><td>YES</td></tr><tr><td>table_locks_waited</td><td>0</td><td>YES</td></tr><tr><td>lockdeadlock=false_positives</td><td>0</td><td>YES</td></tr><tr><td>lockdeadlock_rounds</td><td>37214</td><td>YES</td></tr><tr><td>lockdeadlocks</td><td>1</td><td>YES</td></tr><tr><td>lock_rec Granted Attempts</td><td>1</td><td>YES</td></tr><tr><td>lock_rec_lock_created</td><td>0</td><td>NO</td></tr><tr><td>lock_rec_lock_Removed</td><td>0</td><td>NO</td></tr><tr><td>lock_rec_lock_request</td><td>0</td><td>NO</td></tr><tr><td>lock_rec_lock_waits</td><td>0</td><td>NO</td></tr><tr><td>lock_rec_locks</td><td>0</td><td>NO</td></tr><tr><td>lock_rec_release Attempts</td><td>24317</td><td>YES</td></tr><tr><td>lock_row_lock_current_waits</td><td>0</td><td>YES</td></tr><tr><td>lock_schedule Refreshes</td><td>37214</td><td>YES</td></tr><tr><td>lock_table_lock_created</td><td>0</td><td>NO</td></tr><tr><td>lock_table_lock_Removed</td><td>0</td><td>NO</td></tr><tr><td>lock_table_lock_waits</td><td>0</td><td>NO</td></tr><tr><td>lock_table_lock_s</td><td>0</td><td>NO</td></tr><tr><td>lock Threads Waiting</td><td>0</td><td>YES</td></tr><tr><td>lock_timeout</td><td>1</td><td>YES</td></tr><tr><td>innodb_rwlock_s_os_waits</td><td>12248</td><td>YES</td></tr><tr><td>innodb_rwlock_sspin_rounds</td><td>19299</td><td>YES</td></tr><tr><td>innodb_rwlock_sSpin_waits</td><td>6811</td><td>YES</td></tr><tr><td>innodb_rwlock sx_os_waits</td><td>171</td><td>YES</td></tr><tr><td>innodb_rwlock sxSpin_rounds</td><td>5239</td><td>YES</td></tr><tr><td>innodb_rwlock sxSpin_waits</td><td>182</td><td>YES</td></tr></table>

```txt
innodb_rwlock_x_os_waits 26283 YESinnodb_rwlock_x.spin_rounds 774745 YESinnodb_rwlock_xspin_waits 12666 YES 
```

34 rows in set (0.0174 sec)

The innodb_row_lock_%, lock_deadlocks, and lock_timeouts metrics are the most interesting. The row lock metrics show how many locks are currently waiting and statistics for the amount of time in milliseconds spent on waiting to acquire InnoDB record locks. The lock_deadlocks and lock_timeouts metrics show the number of deadlocks and lock wait timeouts that have been encountered, respectively.

If you encounter InnoDB mutex or semaphore contention, then the innodb_ rwlock_% metrics are useful to monitor the rate the waits happen and how many rounds that are spent waiting.

As you can see, not all of the metrics are enabled by default (these are all InnoDB metrics), so let’s investigate how it is possible to enable and disable the metrics that come from the INNODB_METRICS view.

# Configuring the InnoDB Metrics

The InnoDB metrics can be configured, so you can choose which are enabled, and you can reset the statistics. You enable, disable, and reset the metrics using global system variables:

• innodb_monitor_disable: Disable one or more metrics.   
innodb_monitor_enable: Enable one or more metrics.   
• innodb_monitor_reset: Reset the counter for one or more metrics.   
• innodb_monitor_reset_all: Reset all statistics including the counter, minimum, and maximum values for one or more metrics.

The metrics can be turned on and off as needed with the current status found in the STATUS column of the INNODB_METRICS view. You specify the name of the metric or the name of the subsystem prepended with module_ as the value to the innodb_monitor_ enable or innodb_monitor_disable variable, and you can use $\%$ as a wild card. The value all works as a special value to affect all metrics.

Note When you specify a module, it will only work as expected if there is no metric matching the module. Examples where you cannot specify the module are module_cpu, module_page_track, and module_dblwr.

Listing 2-6 shows an example of enabling and using all the metrics matching icp% (which happens to be the metrics in the icp – index condition pushdown – subsystem). After querying the metrics, they are disabled again using the subsystem as the argument. The values of COUNT depend on the workload you have at the time of the query.

Listing 2-6. Using the INNODB_METRICS view   
-- Connection Processlist ID Thread ID Event ID   
-- 1 32 74 6   
-- Connection 1   
mysql> SET GLOBAL innodb_monitor_enable $=$ 'icp%';   
Query OK, 0 rows affected (0.0003 sec)   
mysql> SELECT NAME, SUBSYSTEM, COUNT, MIN_COUNT, MAX_COUNT, AVG_COUNT, STATUS, COMMENT FROM information_schema.INNODB_METRICS WHERE SUBSYSTEM $=$ 'icp'\G   
********** 1. row ***   
NAME: icp Attempts   
SUBSYSTEM: icp   
COUNT: 0   
MIN_COUNT: NULL   
MAX_COUNT: NULL   
AVG_COUNT: 0   
STATUS: enabled   
COMMENT: Number of attempts for index push-down condition checks

```txt
********** 2. row ***  
NAME: icp_no_MATCH  
SUBSYSTEM: icp  
COUNT: 0  
MIN_COUNT: NULL  
MAX_COUNT: NULL  
AVG_COUNT: 0  
STATUS: enabled  
COMMENT: Index push-down condition does not match  
********** 3. row ***  
NAME: icp_out_of_range  
SUBSYSTEM: icp  
COUNT: 0  
MIN_COUNT: NULL  
MAX_COUNT: NULL  
AVG_COUNT: 0  
STATUS: enabled  
COMMENT: Index push-down condition out of range  
********** 4. row ***  
NAME: icp_match  
SUBSYSTEM: icp  
COUNT: 0  
MIN_COUNT: NULL  
MAX_COUNT: NULL  
AVG_COUNT: 0  
STATUS: enabled  
COMMENT: Index push-down condition matches  
4 rows in set (0.0011 sec)  
mysql> SET GLOBAL innodb_monitor_disable = 'module_icp';  
Query OK, 0 rows affected (0.0004 sec) 
```

First, the metrics are enabled using the innodb_monitor_enable variable; then the values are retrieved. In addition to the values shown, there is also a set of columns with the _RESET suffix which are reset when you set the innodb_monitor_reset (only the counter) or innodb_monitor_reset_all system variable. Finally, the metrics are disabled again.

Caution The metrics have varying overheads, so you are recommended to test with your workload before enabling metrics in production.

# InnoDB Lock Monitor and Deadlock Logging

InnoDB has for a long time had its own lock monitor with the lock information returned in the InnoDB monitor output. By default, the InnoDB monitor includes information about the latest deadlock as well as locks involved in lock waits. By enabling the innodb_ status_output_locks option (disabled by default), all locks will be listed; this is similar to what you have in the Performance Schema data_locks table.

To demonstrate the deadlock and transaction information, you can create a deadlock using the steps in Listing 2-7.

Listing 2-7. An example of creating a deadlock   
-- Connection Processlist ID Thread ID Event ID   
1 19 66 6   
2 20 67 6   
-- Connection 1   
Connection 1> START TRANSACTION;   
Query OK, 0 rows affected (0.0003 sec)   
Connection 1> UPDATE world.city SET Population = Population + 1 WHERE $\mathrm{ID} = 130$ Query OK, 1 row affected (0.0008 sec)   
Rows matched: 1 Changed: 1Warnings: 0   
-- Connection 2   
Connection 2> START TRANSACTION;   
Query OK, 0 rows affected (0.0002 sec)

```txt
Connection 2> UPDATE world.city
    SET Population = Population + 1
    WHERE ID = 3805;
Query OK, 1 row affected (0.0008 sec)
Rows matched: 1 Changed: 1Warnings: 0
Connection 2> UPDATE world.city
    SET Population = Population + 1
    WHERE ID = 130;
-- Connection 1
Connection 1> UPDATE world.city
    SET Population = Population + 1
    WHERE ID = 3805;
2020-06-27 12:54:26.833760 1 [ERROR] mysqlsh.DBError ... 
```

ERROR: 1213: Deadlock found when trying to get lock; try restarting transaction

-- Connection 2 Query OK, 1 row affected (0.1013 sec)

Rows matched: 1 Changed: 1 Warnings: 0

You generate the InnoDB lock monitor output using the SHOW ENGINE INNODB STATUS statement. Listing 2-8 shows an example of enabling all lock information and generating the monitor output after executing the statements in Listing 2-7. (The statements used in Listing 2-8 are included as an investigation for the Listing 2-7 workload in the concurrency_book Python module.) The complete InnoDB monitor output is also available from this book’s GitHub repository in the file listing_2_8.txt.

# Listing 2-8. The InnoDB monitor output

-- Investigation #1   
-- Connection 3   
Connection $3>$ SET GLOBAL innodb_status_output_locks $\equiv$ ON; Query OK, O rows affected (0.0005 sec)   
-- Investigation #3   
Connection $3>$ SHOW ENGINE INNODB STATUS\G

Chapt er 2 Monit oring Locks and Mut exes   
```javascript
\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\* 1. row \*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\* 
```

Type: InnoDB

Name:

Status:

```csv
2020-06-27 12:54:29 0x7f00 INNODB MONITOR OUTPUT 
```

```txt
Per second averages calculated from the last 50 seconds 
```

```txt
BACKGROUND THREAD 
```

```txt
srv/master_thread loops: 2532 sv_ractive, 0 sv_shutdown, 1224 sv_idles  
srv/master_thread log flush and writes: 0 
```

```txt
SEMAPHORES 
```

```txt
OS WAIT ARRAY INFO: reservation count 7750 
```

```txt
OS WAIT ARRAY INFO: signal count 6744 
```

```txt
RW-shared spins 3033,rounds 5292,OS waits 2261 
```

```txt
RW-excl spins 1600, rounds 25565, OS waits 1082 
```

```txt
RW-sx spins 2167, rounds 61634, OS waits 1874 
```

```txt
Spin rounds per wait: 1.74 RW-shared, 15.98 RW-excl, 28.44 RW-sx 
```

LATEST DETECTED DEADLOCK   
```txt
2020-06-27 12:54:26 0x862c 
```

```txt
\*\*\* (1) TRANSACTION: 
```

```txt
TRANSACTION 296726, ACTIVE 0 sec starting index read 
```

```txt
mysql tables in use 1, locked 1 
```

```txt
LOCK WAIT 3 lock struct(s), heap size 1136, 2 row lock(s), undo log entries 1 
```

```txt
MySQL thread id 20, OS thread handle 29332, query id 56150 localhost ::1 root updating 
```

```sql
UPDATE world.city 
```

```txt
SET Population = Population + 1 
```

```txt
WHERE ID = 130 
```

*** (1) HOLDS THE LOCK(S):

RECORD LOCKS space id 259 page no 34 n bits 248 index PRIMARY of table `world`.`city` trx id 296726 lock_mode X locks rec but not gap Record lock, heap no 66 PHYSICAL RECORD: n_fields 7; compact format; info bits 0

0: len 4; hex 80000edd; asc ;;   
1: len 6; hex 000000048716; asc ;;   
2: len 7; hex 020000015f2949; asc _)I;;   
3: len 30; hex 53616e204672616e636973636f202020202020202020202020202020 2020; asc San Francisco ; (total 35 bytes);   
4: len 3; hex 555341; asc USA;;   
5: len 20; hex 43616c69666f726e696120202020202020202020; asc

California

6: len 4; hex 800bda1e; asc ;;

*** (1) WAITING FOR THIS LOCK TO BE GRANTED:

RECORD LOCKS space id 259 page no 7 n bits 248 index PRIMARY of table `world`.`city` trx id 296726 lock_mode X locks rec but not gap waiting Record lock, heap no 44 PHYSICAL RECORD: n_fields 7; compact format; info bits 0

0: len 4; hex 80000082; asc ;;   
1: len 6; hex 000000048715; asc ;;   
2: len 7; hex 01000000d81fcd; asc   
3: len 30; hex 5379646e65792020202020202020202020202020202020202020202020 20; asc Sydney ; (total 35 bytes);   
4: len 3; hex 415553; asc AUS;;   
5: len 20; hex 4e657720536f7574682057616c65732020202020; asc New South Wales ;;   
6: len 4; hex 8031fdb0; asc 1 ;;

*** (2) TRANSACTION:

TRANSACTION 296725, ACTIVE 0 sec starting index read mysql tables in use 1, locked 1 LOCK WAIT 3 lock struct(s), heap size 1136, 2 row lock(s), undo log entries 1 MySQL thread id 19, OS thread handle 6576, query id 56151 localhost ::1 root updating

Chapt er 2 Monit oring Locks and Mut exes   
```txt
UPDATE world.city  
SET Population = Population + 1  
WHERE ID = 3805 
```

```txt
\*\* (2) HOLDS THE LOCK(S): RECORD LOCKS space id 259 page no 7 n bits 248 index PRIMARY of table `world`. `city` trx id 296725 lock_mode X locks rec but not gap Record lock, heap no 44 PHYSICAL RECORD: n_fields 7; compact format; info bits 0 
```

```txt
0: len 4; hex 80000082; asc ; 
```

```asm
1: len 6; hex 000000048715; asc ;; 
```

```txt
2: len 7; hex 01000000d81fcd; asc ;; 
```

```javascript
3: len 30; hex 5379646e657920202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202 
```

```txt
4: len 3; hex 415553; asc AUS;; 
```

```txt
5: len 20; hex 4e657720536f7574682057616c65732020202020; asc New South Wales ; 
```

```javascript
6: len 4; hex 8031fdb0; asc 1 ;; 
```

```txt
*** (2) WAITING FOR THIS LOCK TO BE GRANTED: RECORD LOCKS space id 259 page no 34 n bits 248 index PRIMARY of table `world`. `city` trx id 296725 lock_mode X locks rec but not gap waiting Record lock, heap no 66 PHYSICAL RECORD: n_fields 7; compact format; info bits 0 
```

```txt
0: len 4; hex 80000edd; asc ;; 
```

```asm
1: len 6; hex 000000048716; asc ;; 
```

```asm
2: len 7; hex 020000015f2949; asc ()I;; 
```

```javascript
3: len 30; hex 53616e204672616e636973636f202020202020202020202020202020202020; asc San Francisco ; (total 35 bytes); 
```

```txt
4: len 3; hex 555341; asc USA;; 
```

```txt
5: len 20; hex 43616c69666f726e696120202020202020202020; as  
California ;; 
```

```javascript
6: len 4; hex 800bda1e; asc ;; 
```

*** WE ROLL BACK TRANSACTION (2)

# TRANSACTIONS

Trx id counter 296728

Purge done for trx's n:o < 296728 undo n:o < 0 state: running but idle History list length 1

LIST OF TRANSACTIONS FOR EACH SESSION:

---TRANSACTION 283598406541472, not started

0 lock struct(s), heap size 1136, 0 row lock(s)

---TRANSACTION 283598406540640, not started

0 lock struct(s), heap size 1136, 0 row lock(s)

---TRANSACTION 283598406539808, not started

0 lock struct(s), heap size 1136, 0 row lock(s)

---TRANSACTION 283598406538976, not started

0 lock struct(s), heap size 1136, 0 row lock(s)

---TRANSACTION 296726, ACTIVE 3 sec

3 lock struct(s), heap size 1136, 2 row lock(s), undo log entries 2

MySQL thread id 20, OS thread handle 29332, query id 56150 localhost ::1 root

TABLE LOCK table `world`.`city` trx id 296726 lock mode IX

RECORD LOCKS space id 259 page no 34 n bits 248 index PRIMARY of table

`world`.`city` trx id 296726 lock_mode X locks rec but not gap

Record lock, heap no 66 PHYSICAL RECORD: n_fields 7; compact format; info bits 0

0: len 4; hex 80000edd; asc ;;

1: len 6; hex 000000048716; asc ;;   
2: len 7; hex 020000015f2949; asc _)I;;   
3: len 30; hex 53616e204672616e636973636f202020202020202020202020202020

2020; asc San Francisco ; (total 35 bytes);

4: len 3; hex 555341; asc USA;;   
5: len 20; hex 43616c69666f726e696120202020202020202020; asc

California ，，

6: len 4; hex 800bda1e; asc ;;

RECORD LOCKS space id 259 page no 7 n bits 248 index PRIMARY of table `world`.`city` trx id 296726 lock_mode X locks rec but not gap Record lock, heap no 44 PHYSICAL RECORD: n_fields 7; compact format; info bits 0

0: len 4; hex 80000082; asc ;;   
1: len 6; hex 000000048716; asc ;;   
2: len 7; hex 020000015f296c; asc _l;;   
3: len 30; hex 5379646e65792020202020202020202020202020202020 2020; asc Sydney ; (total 35 bytes);   
4: len 3; hex 415553; asc AUS;;   
5: len 20; hex 4e657720536f7574682057616c657320202020; asc New South Wales ;;   
6: len 4; hex 8031fdb0; asc 1 ;;   
...   
-- Investigation #2   
Connection $3>$ SET GLOBAL innodb_status_output_locks $\equiv$ OFF;   
Query OK, O rows affected (0.0005 sec)

Appendix A includes an overview of the sections that the report consists of.

Near the top is the section LATEST DETECTED DEADLOCK which includes details of the transactions and locks involved in the latest deadlock and when it occurred. If no deadlocks have occurred since the last restart of MySQL, this section is omitted. Chapter 16 includes an example of investigating deadlocks.

Note The deadlock section in the InnoDB monitor output only includes information for deadlocks involving InnoDB record locks. For deadlocks involving non-InnoDB locks such as user-level locks, there is no equivalent information.

A little further down the output, there is the section TRANSACTIONS which lists the InnoDB transactions. Do note that transactions that are not holding any locks (e.g., pure SELECT queries) are not included. In the example, there is an intention exclusive lock held on the world.city table and exclusive locks on the rows with the primary key equal to 3805 (the 80000edd in the record lock information for the first field means the row with the value 0xedd, which is the same as 3805 in decimal notation) and 130 (80000082).

Tip N owadays, the lock information in the InnoDB monitor output is better obtained from the performance_schema.data_locks and performance schema.data_lock_waits tables. The deadlock information is however still very useful.

You can request the monitor output to be dumped every 15 seconds to stderr by enabling the innodb_status_output option. Do note that the output is quite large, so be prepared for your error log to grow quickly if you enable it. The InnoDB monitor output can also easily end up hiding messages about more serious issues. InnoDB also enables outputting the monitor output to the error log automatically when certain conditions apply such as when InnoDB has difficulties finding free blocks in the buffer pool or there are long semaphore waits.

If you want to ensure you record all deadlocks, you can enable the innodb_print_ all_deadlocks option. This causes deadlock information like that in the InnoDB monitor output to be printed to the error log every time a deadlock occurs. This can be useful, if you need to investigate deadlocks, but it is recommended only to enable it on demand to avoid the error log to become very large and potentially hide other problems.

Caution Be careful if you enable regular outputs of the InnoDB monitor or information about all deadlocks. The information may easily hide important messages logged to the error log.

The top of the InnoDB monitor output includes information about semaphore waits which is the last monitoring category to discuss.

# InnoDB Mutexes and Semaphores

InnoDB uses mutual exclusion objects (better known as mutexes)1 and semaphores to guard code paths, for example, to avoid race conditions when updating the buffer pool. There are three resources available for monitoring mutexes in MySQL of which two have already been encountered. The most generic tool is the synchronization waits

in the Performance Schema; however, they are not enabled by default and can cause performance problems to have enabled. This section focuses on the two other resources that are specific to InnoDB.

# Note In InnoDB monitoring there is no clear distinction between mutexes and semaphores.

As seen in the previous section, the InnoDB monitor output contains a semaphores section which shows some general statistics as well as currently waiting semaphores. Listing 2-9 shows an example of the semaphores section with ongoing waits. (It is not trivial to generate semaphore waits on demand, so reproduction steps have not been included. See Chapter 18 for an example of a workload that is likely to cause semaphore waits.)

# Listing 2-9. The InnoDB monitor semaphores section

SEMAPHORES

OS WAIT ARRAY INFO: reservation count 831

--Thread 28544 has waited at buf0buf.cc line 4637 for 0 seconds the semaphore:

Mutex at 000001F1AD24D5E8, Mutex BUF_POOL_LRU_LIST created buf0buf.cc:1228, lock var 1

--Thread 10676 has waited at buf0flu.cc line 1639 for 1 seconds the semaphore:

Mutex at 000001F1AD24D5E8, Mutex BUF_POOL_LRU_LIST created buf0buf.cc:1228, lock var 1

--Thread 10900 has waited at buf0lru.cc line 1051 for 0 seconds the semaphore:

Mutex at 000001F1AD24D5E8, Mutex BUF_POOL_LRU_LIST created buf0buf.cc:1228, lock var 1

--Thread 28128 has waited at buf0buf.cc line 2797 for 1 seconds the semaphore:

Mutex at 000001F1AD24D5E8, Mutex BUF_POOL_LRU_LIST created buf0buf.cc:1228, lock var 1

--Thread 33584 has waited at buf0buf.cc line 2945 for 0 seconds the semaphore:

Mutex at 000001F1AD24D5E8, Mutex BUF_POOL_LRU_LIST created buf0buf.cc:1228, lock var 1

OS WAIT ARRAY INFO: signal count 207

RW-shared spins 51, rounds 86, OS waits 35

RW-excl spins 39, rounds 993, OS waits 35

RW-sx spins 30, rounds 862, OS waits 25

Spin rounds per wait: 1.69 RW-shared, 25.46 RW-excl, 28.73 RW-sx

In this case the first wait is in buf0buf.cc line 4637 which refers to the source code file name and line number where the mutex is requested. The line number depends on the release number you are using, and the compiler/platform can even make line number change by one. The buf0buf.cc refers to which contains the following code in MySQL 8.0.21 around line 4637 (the line number is prefixed each line):

4577 /** Inits a page for read to the buffer buf_pool. If the page is

4578 (1) already in buf_pool, or

4579 (2) if we specify to read only ibuf pages and the page is not an ibuf page, or

4580 (3) if the space is deleted or being deleted,

4581 then this function does nothing.

4582 Sets the io_fix flag to BUF_IO_READ and sets a non-recursive exclusive lock

4583 on the buffer frame. The io-handler must take care that the flag is cleared

4584 and the lock released later.

<table><tr><td>4585 @param[out]</td><td>err</td><td>DB_SUCCESS or DB_TABLESPACE_
DELETED</td></tr><tr><td>4586 @param[in]</td><td>mode</td><td>BUF_READ_IBUF[PAGES_ONLY, ...</td></tr><tr><td>4587 @param[in]</td><td>page_id</td><td>page id</td></tr><tr><td>4588 @param[in]</td><td>page_size</td><td>page size</td></tr><tr><td>4589 @param[in]</td><td>unzip</td><td>TRUE=request uncompressed page</td></tr></table>

Chapt er 2 Monit oring Locks and Mut exes   
```c
4590 @return pointer to the block or NULL \*/   
4591 buf_page_t *buf_page_init_for_read(dberr_t \*err, uint mode,   
4592 const page_id_t &page_id,   
4593 const page_size_t &page_size, ibool unzip) {   
4637 mutex-enter(&buf_pool->LRU_list_mutex);   
... 
```

The function is trying to read a page into the buffer pool and in line 4637 requests the mutex on the LRU list of the buffer pool. This mutex was created in buf0buf.cc:1228 (also seen from the semaphores section). It is the same mutex that all the waits are for, but in different parts of the source. So, this means that there is contention maintaining the least recently used list of the InnoDB buffer pool. (The waits in this case were created by having innodb_buffer_pool_size $=$ 5M while executing concurrent queries on an almost 2 GiB large table.)

Thus, it is in general necessary to reference the source code when investigating semaphore waits. That said, the file name is a good hint in what part of the code the contention is, for example, buf0buf.cc is related to the buffer pool, and buf0flu.cc is related to the buffer pool flushing algorithm.

The semaphores section is useful to see the waits that are ongoing, but it is of little use when monitoring over time. For that the InnoDB mutex monitor is a better option. You access the mutex monitor using the SHOW ENGINE INNODB MUTEX statement:

mysql> SHOW ENGINE INNODB MUTEX;   
```txt
+  
| Type | Name | Status | 
```

The file name and line number refers to where the mutex is created. The mutex monitor is not the most user-friendly tool in MySQL as each mutex may be present multiple times and the waits cannot be summed without parsing the output. However, it is enabled by default, so you can use it at any time.

Note SHOW ENGINE INNODB MUTEX only includes mutexes and rw-lock semaphores that has had at least one OS wait.

The collection of mutex information is enabled and disabled using the latch InnoDB metric (which is hidden, so you cannot see the current value). There is usually no reason to disable the latch metric.

# Summary

In this chapter the resources available for monitoring and investigating locks have been introduced. First the Performance Schema tables were considered. There are dedicated tables for querying the current metadata and data lock requests with information about the object that is the target of the lock, whether it is a shared or exclusive lock and whether the lock request has been granted. At the lowest level, there are also tables that allow you to investigate synchronization waits; however, these are not enabled by default and have a significant overhead. At the opposite end of the granularity scale, the statement tables and error summary tables can be used to investigate which statements encounter errors and the frequency of errors.

Second, the sys schema can also be useful particularly to investigate lock waits issues with the innodb_lock_waits view providing information about ongoing InnoDB data lock waits and schema_table_lock_waits about ongoing table metadata lock waits.

Third, at the highest level, the status counters and InnoDB metrics give an overview of the activity on the instance including the use of locks and failure to obtain locks. If you want more information about InnoDB locks, then the lock monitor provides similar information to the data lock tables in the Performance Schema, but in a less readily usable format, and the InnoDB monitor includes details of the latest occurred deadlock. The InnoDB monitor also includes information about semaphore waits, and finally the InnoDB mutex monitor provides statistics about mutex waits.

Another useful way to get information about the lock usage is to look at the transaction information. This will be considered in the next chapter.

# Monitoring InnoDB Transactions

In the previous chapter, you learned how to find information about locks at a relatively low level. It is also important to include information at a higher level as locks have a duration up to the completion of the transaction. (Exceptions are user locks and explicit table locks which can last for longer.) In MySQL Server, transactions mean InnoDB, and this chapter focuses on monitoring InnoDB transactions.

First the INNODB_TRX view in the Information Schema will be covered. This is often the most important resource when it comes to investigating ongoing transactions. Another source of information about transactions is the InnoDB monitor which you also encountered in the previous chapter. Finally, the metrics in the INNODB_METRICS and the sys.metrics views are discussed.

# Information Schema INNODB_TRX

The INNODB_TRX view in the Information Schema is the most dedicated source of information about InnoDB transactions. It includes information such as when the transaction started, how many rows have been modified, and how many locks are held. The INNODB_TRX view is also used by the sys.innodb_lock_waits view to provide some information about the transactions involved in lock wait issues. Table 3-1 summarizes the columns in the table.

Table 3-1. The columns in the information_schema.INNODB_TRX view   

<table><tr><td>Column/Data Type</td><td>Description</td></tr><tr><td>trx_id</td><td>The transaction id. This can be useful when referring to the transaction or comparing with the output of the InnoDB monitor. Otherwise, the id should be treated purely internal and not be given any significance.</td></tr><tr><td>varchar(18)</td><td></td></tr><tr><td>trx_state</td><td>The state of the transaction. This can be one of RUNNING, LOCK</td></tr><tr><td>varchar(13)</td><td>WAIT, ROLLING BACK, and COMMITTING.</td></tr><tr><td>trx_started</td><td>When the transaction was started using the system time zone.</td></tr><tr><td>datetime</td><td></td></tr><tr><td>trx_requested_</td><td>When thetrx_state is LOCK WAIT, this column shows the id of the lock that the transaction is waiting for.</td></tr><tr><td>lock_id</td><td></td></tr><tr><td>varchar(105)</td><td></td></tr><tr><td>trx_wait_started</td><td>When thetrx_state is LOCK WAIT, this column shows when the lock wait started using the system time zone.</td></tr><tr><td>datetime</td><td></td></tr><tr><td>trx_weight</td><td>A measure of how much work has been done by the transaction in terms of rows modified and locks held. This is the weight that is used to determine which transaction is rolled back in case of a deadlock.</td></tr><tr><td>bigint unsigned</td><td></td></tr><tr><td>trx_msql_thread_id</td><td>The connection id (the same as the PROCESSLIST_ID column in the Performance Schema threads table) of the connection executing the transaction.</td></tr><tr><td>bigint unsigned</td><td></td></tr><tr><td>trx_query</td><td>The query currently executed by the transaction. If the transaction is idle, the query is NULL.</td></tr><tr><td>varchar(1024)</td><td></td></tr><tr><td>trx operation_state</td><td>The current operation performed by the transaction. This may be NULL even when a query is executing.</td></tr><tr><td>varchar(64)</td><td></td></tr><tr><td>trx_tables_in_use</td><td>The number of tables the transaction has used.</td></tr><tr><td>bigint unsigned</td><td></td></tr></table>

(continued)

Table 3-1. (continued)   

<table><tr><td>Column/Data Type</td><td>Description</td></tr><tr><td>trx_tables Lockedbigint unsigned</td><td>The number of tables the transaction holds row locks in.</td></tr><tr><td>trx_lock_structsbigint unsigned</td><td>The number of lock structures created by the transaction.</td></tr><tr><td>trx_lock_memory_bytesbigint unsigned</td><td>The amount of memory in bytes used by the locks held by the transaction.</td></tr><tr><td>trx_rows Lockedbigint unsigned</td><td>The number of record locks held by the transaction. While called row locks, it also includes index locks.</td></tr><tr><td>trx_rows_modifiedbigint unsigned</td><td>The number of rows modified by the transaction.</td></tr><tr><td>trx_concurrencyTicketsbigint unsigned</td><td>When innodb_thread_concurrency is not 0, a transaction is assigned innodb_concurrencyTickets tickets that it can use before it must allow another transaction to perform work. One ticket corresponds to accessing one row. This column shows how many tickets are left.</td></tr><tr><td>trx_isolation_levelvarchar(16)</td><td>The transaction isolation level used for the transaction.</td></tr><tr><td>trx_unique_checksintr</td><td>Whether the unique_checks variable is enabled for the connection.</td></tr><tr><td>trx_foreign_key_checksintr</td><td>Whether the foreign_key_checks variable is enabled for the connection.</td></tr><tr><td>trx_last_foregnkey_errorvarchar(256)</td><td>The error message of the last (if any) foreign key error encountered by the transaction.</td></tr><tr><td>trx_adaptive_hash_latchedint</td><td>Whether the transaction has locked a part of the adaptive hash index. There is a total of innodb_adaptive_hash_index_parts parts. This column is effectively a Boolean value.</td></tr></table>

(continued)

Table 3-1. (continued)   

<table><tr><td>Column/Data Type</td><td>Description</td></tr><tr><td>trx_adaptive_hash_timeoutbigint unsigned</td><td>Whether to keep the lock on the adaptive hash index across multiple queries. If there is only one part for the adaptive hash index and there is no contention, then the timeout counts down, and the lock is released when the timeout reaches 0. When there is contention or there are multiple parts, the lock is always released after each query, and the timeout value is 0.</td></tr><tr><td>trx_is_read_onlyint</td><td>Whether the transaction is a read-only transaction. A transaction can be read-only either by declaring it explicitly or for single-statement transactions with autocommit enabled where InnoDB can detect that the query will only read data.</td></tr><tr><td>trx_autocommit_non lockingint</td><td>When the transaction is a single-statement non-locking SELECT and the autocommit option is enabled, this column is set to 1. When both this column andtrx_is_read_only are 1, InnoDB can optimize the transaction to reduce the overhead.</td></tr><tr><td>trx_schedule_weightbigint unsigned</td><td>The transaction weight that is assigned to the transaction by the Contention-Aware Transaction Scheduling (CATS) algorithm (see Chapter 8). The value only has a meaning for transactions in the LOCK WAIT state. This column was added in 8.0.20.</td></tr></table>

The information available from the INNODB_TRX view makes it possible to determine which transactions have the greatest impact. Listing 3-1 shows an example of starting two transactions that can be investigated.

Listing 3-1. Example transactions   

<table><tr><td>-- Connection</td><td>Processlist</td><td>ID</td><td>Thread ID</td><td>Event ID</td></tr><tr><td>--</td><td>1</td><td>53</td><td>163</td><td>6</td></tr><tr><td>--</td><td>2</td><td>54</td><td>164</td><td>6</td></tr></table>

-- Connection 1 Connection 1> START TRANSACTION; Query OK, 0 rows affected (0.0002 sec)

```txt
Connection 1> UPDATE world.city SET Population = Population + MOD(ID, 2) + SLEEP(0.01);  
-- Connection 2  
Connection 2> SET SESSION autocommit = ON;  
Query OK, 0 rows affected (0.0004 sec) 
```

Connection 2> SELECT COUNT(*) FROM world.city WHERE ID > SLEEP(0.01);

The transactions will run for 40–50 seconds. While they are executing, you can query the INNODB_TRX view like it is shown in Listing 3-2 (the exact data depends on the ids in your test and when you query the INNODB_TRX view).

Listing 3-2. Example output of the INNODB_TRX view   
-- Investigation #1   
-- Connection 3   
Connection $3>$ SELECT \* FROM information_schema.INNODB_TRX WHEREtrx_mysql_thread_id IN (53, 54)\G   
********** 1. row \*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\* trx_id: 296813 trx_state: RUNNING trx_started: 2020-06-27 17:46:10   
trx_requested_lock_id: NULL trx_wait_started: NULL trx_weight: 1023   
trx_mysql_thread_id: 53 trx_query: UPDATE world.city SET Population = Population + MOD(ID, 2) + SLEEP(0.01)   
trx operation state: NULL trx_tables_in_use: 1 trx_tablesLocked: 1 trx_lock_structs: 14   
trx_lock_memory_bytes: 1136   
trx_rows Locked: 2031   
trx_rows_modified: 1009

Chapt er 3 Mo nito ring Inno DB Transactio ns   
```txt
trx_concurrencyTickets: 0  
trx_isolation_level: REPEATABLE READ  
trx_unique_checks: 1  
trx_foreign_key_checks: 1  
trx_lastFOREIGN_key_error: NULL  
trx_adaptive_hash_latched: 0  
trx_adaptive_hash_timeout: 0  
trx_is_read_only: 0  
trx_autocommit_non_locking: 0  
trx_schedule_weight: NULL  
********** 2. row******************  
trx_id: 283598406543136  
trx_state: RUNNING  
trx_started: 2020-06-27 17:46:10  
trx_requested_lock_id: NULL  
trx_wait_started: NULL  
trx_weight: 0  
trx_mysql_thread_id: 54  
trx_query: SELECT COUNT(*) FROM world.city WHERE ID > SLEEP(0.01)  
trx_operation_state: NULL  
trx_tables_in_use: 1  
trx_tablesLocked: 0  
trx_lock_structs: 0  
trx_lock_memory_bytes: 1136  
trx_rows Locked: 0  
trx_rows_modified: 0  
trx_concurrencyTickets: 0  
trx_isolation_level: REPEATABLE READ  
trx_unique_checks: 1  
trx.Foreign_key_checks: 1  
trx_lastFOREIGN_key_error: NULL  
trx_adaptive_hash_latched: 0  
trx_adaptive_hash_timeout: 0  
trx_is_read_only: 1 
```

trx_autocommit_non_locking: 1 trx_schedule_weight: NULL

2 rows in set (0.0008 sec)

The first row shows an example of a transaction that modifies data. At the time the information is retrieved, 1009 rows have been modified, and there are around twice as many record locks. You can also see that the transaction is still actively executing a query (an UPDATE statement).

The second row is an example of a SELECT statement executed with autocommit enabled. Since autocommitting is enabled, there can only be one statement in the transaction (an explicit START TRANSACTION disables autocommitting). The trx_query column shows it is a SELECT COUNT(*) query without any lock clauses, so it is a read-only statement. This means that InnoDB can skip some things such as preparing to hold lock and undo information for the transaction which reduces the overhead of the transaction. The trx_autocommit_non_locking column is set to 1 to reflect that.

Which transactions you should be worried about depends on the expected workload on your system. If you have an OLAP workload, it is expected that there will be relatively long-running SELECT queries. For a pure OLTP workload, any transaction running for more than a second and modifying more than a handful of rows may be a sign of problems. For example, to find transactions that are older than 10 seconds, you can use the following query:

SELECT * FROM information_schema.INNODB_TRX WHERE trx_started < NOW() - INTERVAL 10 SECOND;

You can optionally join on other tables such as threads and events_statements_ current in the Performance Schema. An example of this is shown in Listing 3-3.

# Listing 3-3. Querying details of old transactions

-- Investigation #3 Connection 3> SELECT thd.thread_id, thd.processlist_id, trx.trx_id, stmt.event_id, trx.trx_started, TO_SECONDS(NOW()) - TO_SECONDS(trx.trx_started ) AS age_seconds,

Chapt er 3 Mo nito ring Inno DB Transactio ns   
```txt
trx.trx_rows_locked, trx.trx_rows_modified, FORMAT_PICO_TIME(mt timer_wait) AS latency, stmt rows examined, stmt rows affected, sys.format_statement (SQL TEXT) as statement FROM information_schema.INNODB_TRX trx INNER JOIN performance_schema threads thd ON thd(processid = trx.trx_mysql_thread_id INNER JOIN performance_schema.events statements current stmt USING (thread_id) WHERE trx_started < NOW() - INTERVAL 10 SECOND\G   
********** 1. row ***   
thread_id: 163 processlist_id: 53trx_id: 296813 event_id: 9trx_started: 2020-06-27 17:46:10 ageSeconds: 25trx_rows_locked: 2214   
trx_rows_modified: 1100 latency: 25.24 s rows_examined: 2201 rows_aftected: 0 statement: UPDATE world.city SET Populati ... ion + MOD(ID, 2) + SLEEP(0.01)   
********** 2. row ***   
thread_id: 164 processlist_id: 54trx_id: 283598406543136 event_id: 8trx_started: 2020-06-27 17:46:10 ageSeconds: 25   
trx_rows_locked: 0   
trx_rows_modified: 0 
```

```txt
latency: 25.14 s  
rows_examined: 0  
rows_aftected: 0  
    statement: SELECT COUNT(*) FROM world.city WHERE ID > SLEEP(0.01)  
2 rows in set (0.0021 sec) 
```

You can join to the tables and choose the columns of relevance for your investigation.

Related to the INNODB_TRX view is the transaction list in the InnoDB monitor.

# InnoDB Monitor

The InnoDB monitor is a kind of Swiss army knife of InnoDB information and also includes information about transactions. The TRANSACTIONS section in the output from the InnoDB monitor is dedicated to transactional information. This information does include not only a list of transactions but also the history list length. Listing 3-4 shows an excerpt of the InnoDB monitor with the example of the transaction section taken just after the previous output from the INNODB_TRX view.

# Listing 3-4. Transaction information from the InnoDB monitor

```txt
-- Investigation #4   
Connection 3> SHOW ENGINE INNODB STATUS\G   
**********1. row**********   
Type: InnoDB   
Name:   
Status:   
2020-06-27 17:46:36 0x5784 INNODB MONITOR OUTPUT   
Per second averages calculated from the last 20 seconds   
... 
```

Trx id counter 296814   
```txt
TRANSACTIONS 
```

Purge done for trx's n:o < 296813 undo n:o < 0 state: running but idle History list length 1   
UPDATE world.city SET Population $=$ Population + MOD(ID, 2) + SLEEP(0.01) ...   
```txt
LIST OF TRANSACTIONS FOR EACH SESSION:  
---TRANSACTION 283598406541472, not started  
0 lock struct(s), heap size 1136, 0 row lock(s)  
---TRANSACTION 283598406540640, not started  
0 lock struct(s), heap size 1136, 0 row lock(s)  
---TRANSACTION 283598406539808, not started  
0 lock struct(s), heap size 1136, 0 row lock(s)  
---TRANSACTION 283598406538976, not started  
0 lock struct(s), heap size 1136, 0 row lock(s)  
---TRANSACTION 296813, ACTIVE 26 sec fetching rows  
mysql tables in use 1, locked 1  
15 lock struct(s), heap size 1136, 2333 row lock(s), undo log entries 1160  
MySQL thread id 53, OS thread handle 23748, query id 56574 localhost ::1  
root User sleep 
```

The top of the TRANSACTIONS section shows the current value of the transaction id counter followed by information of what has been purged from the undo logs. It shows that the undo logs for transaction ids less than 296813 have been purged. The further this purge is behind, the larger the history list length (in the third line of the section) is. Reading the history list length from the InnoDB monitor output is the traditional way to get the length of the history list. In the next section, it will be shown how to get the value in a better way when used for monitoring purposes.

The rest of the section is a list of transactions. Notice that while the output is generated with the same two active transactions as were found in INNODB_TRX, the transaction list only includes one active transaction (the one for the UPDATE statement). In MySQL 5.7 and later, read-only non-locking transactions are not included in the InnoDB monitor transaction list. For this reason, it is better to use the INNODB_TRX view, if you need to include all active transactions.

As mentioned, there is an alternative way to get the history list length. You need to use the InnoDB metrics for this.

# INNODB_METRICS and sys.metrics

The InnoDB monitor report is useful for a database administrator to get an overview of what is going on in InnoDB, but for monitoring it is not as useful as it requires parsing to get out the data in a way monitoring can use it. You saw earlier in the chapter how the information about the transactions can be obtained from the information_schema. INNODB_TRX view, but how about metrics such as the history list length?

The InnoDB metric system includes several metrics that show information about the transactions in the information_schema.INNODB_METRICS view. These metrics are all located in the transaction subsystem. Listing 3-5 shows a list of the transaction metrics, whether they are enabled by default, and a brief comment explaining what the metric measures.

Listing 3-5. InnoDB metrics related to transactions   
-- Connection Processlist ID Thread ID Event ID   
-- 1 56 166 6   
-- Connection 1   
Connection 1> SELECT NAME, COUNT, STATUS, COMMENT FROM information_schema.INNODB_METRICS WHERE SUBSYSTEM $=$ 'transaction'\G   
********** 1. row   
NAME:trx_rw_commits   
COUNT:0   
STATUS:disabled   
COMMENT: Number of read-write transactions committed   
********** 2. row   
NAME:trx_ro_commits   
COUNT:0   
STATUS:disabled   
COMMENT: Number of read-only transactions committed

Chapt er 3 Mo nito ring Inno DB Transactio ns   
```txt
********** 3. row ***  
NAME: trx_nl_ro commits  
COUNT: o  
STATUS: disabled  
COMMENT: Number of non-locking auto-commit read-only transactions committed  
********** 4. row ***  
NAME: trx_commits_insert_update  
COUNT: o  
STATUS: disabled  
COMMENT: Number of transactions committed with inserts and updates  
********** 5. row ***  
NAME: trxROLLbacks  
COUNT: o  
STATUS: disabled  
COMMENT: Number of transactions rolled back  
********** 6. row ***  
NAME: trxROLLbacks_savepoint  
COUNT: o  
STATUS: disabled  
COMMENT: Number of transactions rolled back to savepoint  
********** 7. row ***  
NAME: trxROLLback.active  
COUNT: o  
STATUS: disabled  
COMMENT: Number of resurrected active transactions rolled back  
********** 8. row ***  
NAME: trx.active_transactions  
COUNT: o  
STATUS: disabled  
COMMENT: Number of active transactions  
********** 9. row ***  
NAME: trx_on_log_no_waits  
COUNT: o  
STATUS: disabled  
COMMENT: Waits for redo during transaction commits 
```

*************************** 10. row ***************************

NAME: trx_on_log_waits

COUNT: 0

STATUS: disabled

COMMENT: Waits for redo during transaction commits

*************************** 11. row ***************************

NAME: trx_on_log_wait_loops

COUNT: 0

STATUS: disabled

COMMENT: Waits for redo during transaction commits

*************************** 12. row ***************************

NAME: trx_rseg_history_len

COUNT: 9

STATUS: enabled

COMMENT: Length of the TRX_RSEG_HISTORY list

*************************** 13. row ***************************

NAME: trx_undo_slots_used

COUNT: 0

STATUS: disabled

COMMENT: Number of undo slots used

*************************** 14. row ***************************

NAME: trx_undo_slots_cached

COUNT: 0

STATUS: disabled

COMMENT: Number of undo slots cached

*************************** 15. row ***************************

NAME: trx_rseg_current_size

COUNT: 0

STATUS: disabled

COMMENT: Current rollback segment size in pages

15 rows in set (0.0012 sec)

The most important of these metrics is trx_rseg_history_len which is the history list length. This is also the only metric that is enabled by default. The metrics related to commits and rollbacks can be used to determine how many read-write, read-only, and non-locking read-only transactions you have and how often they are committed

and rolled back. Many rollbacks suggest there is a problem. If you suspect the redo log is a bottleneck, the trx_on_log_% metrics can be used to get a measure of how much transactions are waiting for the redo log during transaction commits.

Tip You enable InnoDB metrics with the innodb_monitor_enable option and disable them with innodb_monitor_disable. This can be done dynamically.

An alternative and convenient way to query the InnoDB metrics is to use the sys. metrics view which also includes the global status variables. Listing 3-6 shows an example of using the sys.metrics view to obtain the current values and whether the metric is enabled.

Listing 3-6. Using the sys.metrics view to get the transaction metrics   
```sql
-- Connection Processlist ID Thread ID Event ID
-- 1 52 125 6
-- Connection 1
Connection 1> SELECT Variable_name AS Name,
Variable_value AS Value,
Enabled
FROM sys.metrics
WHERE Type = 'InnoDB Metrics - transaction';
+
| Name | Value | Enabled |
+
| trx.active_transactions | 0 | NO |
| trx_commits_insert_update | 0 | NO |
| trx_nl_ro_commits | 0 | NO |
| trx_on_log_no_waits | 0 | NO |
| trx_on_log_wait_loops | 0 | NO |
| trx_on_log_waits | 0 | NO |
| trx_ro_commits | 0 | NO |
| trx_rollback-active | 0 | NO |
| trx_rollback | 0 | NO | 
```

```txt
| trx_rollback_savepoint | 0 | NO |
| --- | --- | --- |
| trx_rseg_current_size | 0 | NO |
| trx_rseg_history_len | 16 | YES |
| trx_rw_commits | 0 | NO |
| trx UndoSlotsCached | 0 | NO |
| trx UndoSlots_used | 0 | NO | 
```

15 rows in set (0.0089 sec)

This shows that the history list length is 16 which is a good low value, so there is next to none overhead from the undo logs. The rest of the metrics are disabled.

# Summary

This chapter has covered how you can obtain information about InnoDB transactions. The primary source of detailed information is the INNODB_TRX view in the Information Schema which includes details such as when the transaction was started, the number of locked and modified rows, etc. You can optionally join on the Performance Schema tables to get more information about the transaction.

You can also use the InnoDB monitor to get information about locking transactions; however, in general, it is preferred to use the INNODB_TRX view. If you are looking for higher-level aggregate statistics, you can use the information_schema.INNODB_METRICS view or alternatively the sys.metrics view. The most commonly used metric is trx_ rseg_history_len which shows the history list length.

Thus far, the discussion of transaction information has been about aggregate statistics either for all transactions or individual transactions. If you want to go deeper into what work a transaction has done, you need to use the Performance Schema as discussed in the next chapter.

# Transactions in the Performance Schema

The Performance Schema supports transaction monitoring in MySQL 5.7 and later, and it is enabled by default in MySQL 8. There are not many transaction details other than related to XA transactions and savepoints available in the Performance Schema that cannot be obtained from the INNODB_TRX view in the Information Schema. However, the Performance Schema transaction events have the advantage that you can combine them with other event types such as statements to get information about the work done by a transaction. This is the main focus of this chapter. Additionally, the Performance Schema offers summary tables with aggregate statistics.

# Transaction Events and Their Statements

The main tables for investigating transactions in the Performance Schema are the transaction event tables. There are three tables for recording current or recent transactions: events_transactions_current, events_transactions_history, and events_transactions_history_long. They have the columns as summarized in Table 4-1.

Table 4-1. The columns of the non-summary transaction event tables   

<table><tr><td>Column/Data Type</td><td>Description</td></tr><tr><td>THREAD_ID</td><td>The Performance Schema thread id of the connection executing the transaction.</td></tr><tr><td>bigint unsigned</td><td></td></tr><tr><td>EVENT_ID</td><td>The event id for the event. You can use the event id to order the events for a thread or as a foreign key together with the thread id between event tables.</td></tr><tr><td>bigint unsigned</td><td></td></tr><tr><td>END_EVENT_ID</td><td>The event id when the transaction completed. If the event id is NULL, the transaction is still ongoing.</td></tr><tr><td>bigint unsigned</td><td></td></tr><tr><td>EVENT_NAME</td><td>The transaction event name. Currently this column always has the value transaction.</td></tr><tr><td>varchar(128)</td><td></td></tr><tr><td>STATE</td><td>The state of the transaction. Possible values are ACTIVE, COMMITTED, and ROLLED BACK.</td></tr><tr><td>enum</td><td></td></tr><tr><td>TRX_ID</td><td>This is currently unused and will always be NULL.</td></tr><tr><td>bigint unsigned</td><td></td></tr><tr><td>GTID</td><td>The GTID for the transaction. When the GTID is automatically determined (the usual), AUTOMATIC is returned. This is the same as the gtid_next variable for the connection executing the transaction.</td></tr><tr><td>varchar(64)</td><td></td></tr><tr><td>XID_FORMAT_ID</td><td>ForXA transactions, the format id.</td></tr><tr><td>int</td><td></td></tr><tr><td>XID_GTRID</td><td>ForXA transactions, the gtrid value.</td></tr><tr><td>varchar(130)</td><td></td></tr><tr><td>XID_BQUAL</td><td>ForXA transactions, the bqual value.</td></tr><tr><td>varchar(130)</td><td></td></tr><tr><td>XA_STATE</td><td>For aXA transaction, the state of the transaction. This can be ACTIVE, IDLE, PREPARED, ROLLED BACK, or COMMITTED.</td></tr><tr><td>varchar(64)</td><td></td></tr><tr><td>SOURCE</td><td>The source code file and line number where the event was recorded.</td></tr><tr><td>varchar(64)</td><td></td></tr><tr><td>TIMER_START</td><td>The time in picoseconds when the event started.</td></tr><tr><td>bigint unsigned</td><td></td></tr></table>

(continued)

Table 4-1. (continued)   

<table><tr><td>Column/Data Type</td><td>Description</td></tr><tr><td>TIMER_END</td><td>The time in picoseconds when the event completed. If the transaction has not completed yet, the value corresponds to the current time.</td></tr><tr><td>bigint unsigned</td><td></td></tr><tr><td>TIMER_WAIT</td><td>The total time in picoseconds it took to execute the event. If the event has not completed yet, the value corresponds to how long the transaction has been active.</td></tr><tr><td>bigint unsigned</td><td></td></tr><tr><td>ACCESS_MODE</td><td>Whether the transaction is in read-only (READ ONLY) or in read-write (READ WRITE) mode.</td></tr><tr><td>enum</td><td>The transaction isolation level for the transaction.</td></tr><tr><td>ISOLATION_LEVEL</td><td>Whether the transaction is autocommiting based on the autocommit option and whether an explicit transaction has been started. Possible values are NO and YES.</td></tr><tr><td>varchar(64)</td><td></td></tr><tr><td>AUTOCOMMIT</td><td>Whether the transaction is autocommiting based on the autocommit option and whether an explicit transaction has been started. Possible values are NO and YES.</td></tr><tr><td>enum</td><td></td></tr><tr><td>NUMBER_OF_SAVEPOINTS</td><td>The number of savepoints created in the transaction.</td></tr><tr><td>bigint unsigned</td><td></td></tr><tr><td>NUMBER_OF_ROLLBACK_TO_SAVEPOINT</td><td>The number of times the transaction has rolled back to a savepoint.</td></tr><tr><td>bigint unsigned</td><td></td></tr><tr><td>NUMBER_OFrelease_SAVEPOINT</td><td>The number of times the transaction has released a savepoint.</td></tr><tr><td>bigint unsigned</td><td></td></tr><tr><td>OBJECT instancesBEGIN</td><td>This field is currently unused and always set to NULL.</td></tr><tr><td>bigint unsigned</td><td></td></tr><tr><td>NESTING_EVENT_ID</td><td>The event id of the event that triggered the transaction.</td></tr><tr><td>bigint unsigned</td><td></td></tr><tr><td>NESTING_EVENT_TYPEenum</td><td>The event type of the event that triggered the transaction.</td></tr></table>

If you are working with XA transactions, the transaction event tables are great when you need to recover a transaction as the format id, gtrid, and bqual values are directly available from the tables, unlike for the XA RECOVER statement where you have to parse the output. In the same way, if you work with savepoints, you can get statistics on the savepoint usage. Otherwise, the information is very similar to what is available in the information_schema.INNODB_TRX view.

For an example of using the events_transactions_current table, you can start two transactions as shown in Listing 4-1.

# Listing 4-1. Example transactions

<table><tr><td>-- Connection</td><td>Processlist ID</td><td>Thread ID</td><td>Event ID</td></tr><tr><td>--</td><td>1</td><td>57</td><td>140</td></tr><tr><td>--</td><td>2</td><td>58</td><td>141</td></tr></table>

-- Connection 1 Connection 1> START TRANSACTION; Query OK, 0 rows affected (0.0004 sec)

Connection 1> UPDATE world.city SET Population $=$ 5200000 WHERE $\mathrm { I D } ~ = ~ 1 3 0$ ;

Connection 1> UPDATE world.city SET Population $=$ 4900000 WHERE $\mathrm { I D } ~ = ~ 1 3 1$ ;

Connection 1> UPDATE world.city SET Population $=$ 2400000 WHERE $\mathrm { I D } ~ = ~ 1 3 2$

Connection 1> UPDATE world.city SET Population $=$ 2000000 WHERE $\mathrm { I D } ~ = ~ 1 3 3$ ;

-- Connection 2 Connection 2> XA START 'abc', 'def', 1;

Connection 2> UPDATE world.city SET Population $=$ 900000 WHERE ID = 3805;

The first transaction is a normal transaction that updates the population of several cities, and the second transaction is an XA transaction. Listing 4-2 shows an example output of the events_transactions_current table listing the currently active transactions.

Listing 4-2. Using the events_transactions_current table   
```sql
-- Investigation #1
-- Connection 3
Connection 3> SELECT *
FROM performance_schema.events_transactions_current
WHERE state = 'ACTIVE'\G
********** 1. row *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** ******
THREAD_ID: 140
EVENT_ID: 8
END_EVENT_ID: NULL
EVENT_NAME: transaction
STATE: ACTIVE
TRX_ID: NULL
GTID: AUTOMATIC
XID_FORMAT_ID: NULL
XID_GTRID: NULL
XID_BQUAL: NULL
XA_STATE: NULL
SOURCE: transaction.cc:209
TIMER_START: 72081362554600000
TIMER_END: 72161455792800000
TIMER_WAIT: 80093238200000
ACCESS_MODE: READ WRITE
ISOLATION_LEVEL: REPEATABLE READ
AUTOCOMMIT: NO
NUMBER_OF_SAVEPOINTS: 0
NUMBER_OF_ROLLBACK_TO_SAVEPOINT: 0
NUMBER_OF_RELEASE_SAVEPOINT: 0
OBJECT_INSTANCE_BEGIN: NULL
NESTING_EVENT_ID: 7
NESTING_EVENT_TYPE: STATEMENT
********** 2. row **** ** **** ** **** ** **** ** **** ** **** ** **** ** **** ** **** ** **** ** **** ** **** ** **** ** **** ** **** ** **** ** **** ** **** ** **** ** **** ** **** ** **** ** **** ** **** ** **** ** **** ** **** ** **** ** **** ** **** ** **** ** **** ** **** ** **** ** **** ** **** ** **** ** **** ** **** ** **** ** **** ** **** ** **** ** **** ** **** ** **** ** **** ** **** ** **** ** **** ** **** * 
```

Chapt er 4 Transacti ons in t he Perf ormance Schema   
```txt
EVENT_NAME: transaction  
STATE: ACTIVE  
TRX_ID: NULL  
GTID: AUTOMATIC  
XID_FORMAT_ID: 1  
XID_GTRID: abc  
XID_BQUAL: def  
XA_STATE: ACTIVE  
SOURCE: transaction.cc:209  
TIMER_START: 72081766957700000  
TIMER_END: 72161455799300000  
TIMER_WAIT: 79688841600000  
ACCESS_MODE: READ WRITE  
ISOLATION_LEVEL: REPEATABLE READ  
AUTOCOMMIT: NO  
NUMBER_OF_SAVEPOINTS: 0  
NUMBER_OF_ROLLBACK_TO_SAVEPOINT: 0  
NUMBER_OF_RELEASE_SAVEPOINT: 0  
OBJECTInstance_BEGIN: NULL  
NESTING_EVENT_ID: 7  
NESTING_EVENT_TYPE: STATEMENT  
2 rows in set (0.0007 sec) 
```

The transaction in row 1 is a regular transaction, whereas the transaction in row 2 is an XA transaction. Both transactions were started by a statement which can be seen from the nesting event type. If you want to find the statement that triggered the transaction, you can use that to query the events_statements_history table like

-- Investigation #2   
Connection 3> SELECT sql_text FROM performance_schema.events_statement史 history WHERE thread_id $= 140$ AND event_id $= 7\backslash G$ ********** 1. row*****

This shows that the transaction executed by thread_id = 140 was started using a START TRANSACTION statement. Since the events_statements_history table only includes the last ten statements for the connection, it is not guaranteed that the statement that started the transaction is still in the history table. If you are looking at a single-statement transaction or the first statement (while it is still executing) when autocommit is disabled, you will need to query the events_statements_current table instead.

The relationship between transactions and statements also goes the other way.

Given a transaction event id and the thread id, you can query the last ten statements executed for that transaction using the statement event history and current tables. Listing 4-3 shows an example for thread_id = 140 and transaction EVENT_ID = 8 (from row 1 of Listing 4-2) where both the statement starting the transaction and subsequent statements are included.

Listing 4-3. Finding the last ten statements executed in a transaction   
-- Investigation #4   
Connection 3> SET @thread_id = 140, @event_id = 8, @nesting_event_id = 7;   
Query OK, 0 rows affected (0.0007 sec)   
-- Investigation #6   
Connection 3> SELECT event_id, sql_text, FORMAT_PICO_TIME(timer_wait) AS latency, IF(end_event_id IS NULL, 'YES', 'NO') AS current FROM ((SELECT event_id, end_event_id, timer_wait, sql_text, nesting_event_id, nesting_event_type FROM performance_schema.events_statementst_current WHERE thread_id $=$ @thread_id )UNION ( SELECT event_id, end_event_id, timer_wait, sql_text, nesting_event_id, nesting_event_type

FROM performance_schema.events_statementsthistory WHERE thread_id $=$ @thread_id ）eventsWHERE (nesting_event_type $\equiv$ 'TRANSACTION' AND nesting_event_id $=$ @event_id) OR event_id $=$ @nesting_event_id ORDER BY event_id DESC\G   
**********1. row*****   
event_id:12   
sql_text:UPDATE world.city SET Population $=$ 2000000 WHERE ID $=$ 133 latency:384.00 us current:NO   
**********2. row*****   
event_id:11   
sql_text:UPDATE world.city SET Population $=$ 2400000 WHERE ID $=$ 132 latency:316.20 us current:NO   
**********3. row*****   
event_id:10   
sql_text:UPDATE world.city SET Population $=$ 4900000 WHERE ID $=$ 131 latency:299.30 us current:NO   
**********4. row*****   
event_id:9   
sql_text:UPDATE world.city SET Population $=$ 5200000 WHERE ID $=$ 130 latency:176.95 ms current: NO   
**********5. row*****   
event_id:7   
sql_text: start transaction latency:223.20 us current:NO   
5 rows in set (0.0016 sec)

The subquery (a derived table) finds all statement events for the thread from the events_statements_current and events_statements_history tables. It is necessary to include the current events as there may be an ongoing statement for the transaction. The statements are filtered by either being a child of the transaction or the nesting event for the transaction (event_id = 7). This will include all statements beginning with the one starting the transactions. There will be up to 11 statements if there is an ongoing statement and otherwise up to ten.

The end_event_id is used to determine whether the statement is currently executing, and the statements are ordered in reverse using the event_id, so the most recent statement is in row 1 and the oldest (the START TRANSACTION statement) in row 5.

This type of query is not only useful to investigate transactions still executing queries. It can also be very useful when you encounter an idle transaction and you want to know what the transaction did before it was left abandoned. Another related way to look for active transactions is to use the sys.session view which uses the events_ transactions_current table to include information about the transactional state for each connection. Listing 4-4 shows an example of querying for active transactions excluding the row for the connection executing the query.

Listing 4-4. Finding active transactions with sys.session   
-- Investigation #7   
Connection 3> SELECT \* FROM sys.session WHERE trx_state $=$ ACTIVE' AND conn_id <> CONNECTION_ID()\G   
**********1. row*******1. row*******1. row*******1. row*******1. row*******1. row*******1. row*******1. row*******1. row*******1. row*******1. row*******1. row*******1. row*******1. row*******1. row*******1. row*******1. row*******1. row*******1. row*******1. row*******1. row*******1. row*******1. row*******1. row*******1. row*******1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*******1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row******1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****

Chapt er 4 Transacti ons in t he Perf ormance Schema   
```txt
progress: NULL lock-latency: 111.00 us rows_examined: 1 rows_sent: 0 rows_aftected: 1 tmp_tables: 0 tmp_disk_tables: 0 full_scan: NO last_statement: UPDATE world.city SET Population = 2000000 WHERE ID = 133 last_statement-latency: 384.00 us current_memory: 228.31 KiB last_wait: NULL last_wait-latency: NULL source: NULLtrx-latency: 7.48 mintrx_state: ACTIVEtrx_autocommit: NO pid: 30936 program_name: mysqlsh   
********** 2. row********** thd_id: 141 conn_id: 58 user: mysqlx/worker db: NULL command: Sleep state: NULL time: 449 current_statement: UPDATE world.city SET Population = 900000 WHERE ID = 3805 statement-latency: NULL progress: NULL lock latency: 387.00 us rows_examined: 1 rows_sent: 0 
```

```txt
rows_aftected: 1  
tmp_tables: 0  
tmp_disk_tables: 0  
full_scan: NO  
last_statement: UPDATE world.city SET Population = 900000  
WHERE ID = 3805  
last_statement-latency: 49.39 ms  
current_memory: 70.14 KiB  
last_wait: NULL  
last_wait-latency: NULL  
source: NULL  
trx-latency: 7.48 min  
trx_state: ACTIVE  
trx_autocommit: NO  
pid: 30936  
program_name: mysqlsh  
2 rows in set (0.0422 sec) 
```

This shows that the transaction in the first row has been active for more than 7 minutes and it is 449 seconds (7.5 minutes) since the last query was executed (your values will differ). The last_statement can be used to determine the last query executed by the connection. This is an example of an abandoned transaction which prevents InnoDB from purging its undo logs. The most common causes of abandoned transactions are a database administrator starting a transaction interactively and getting distracted or that autocommit is disabled and it is not realized a transaction was started.

Caution If you disable autocommit, be careful always to commit or roll back at the end of the work. Some connectors disable autocommit by default, so be aware that your application may not be using the server default.

You can roll the transactions back to avoid changing any data (if you use the MySQL Shell script to reproduce the example, this is done automatically when hitting enter without an answer for the next investigation). For the first (normal) transaction

```sql
-- Connection 1  
Connection 1> ROLLBACK;  
Query OK, 0 rows affected (0.0303 sec) 
```

And for the XA transaction:

```sql
-- Connection 2  
Connection 2> XA END 'abc', 'def', 1;  
Query OK, 0 rows affected (0.0002 sec)  
Connection 2> XA ROLLBACK 'abc', 'def', 1;  
Query OK, 0 rows affected (0.0308 sec) 
```

Another way the Performance Schema tables are useful for analyzing transactions is to use the summary tables to obtain aggregate data.

# Transaction Summary Tables

In the same way as there are statement summary tables that can be used to get reports of the statements that are executed, there are transaction summary tables that can be used to analyze the use of transactions. While they are not quite as useful as their statement counterparts, they do offer insight into which connections and accounts that use transactions in different ways.

There are five transaction summary tables grouping the data globally or by account, host, thread, or user. All of the summaries also group by the event name, but as currently there is only one transaction event (transaction), it is a nil operation. The tables are

events_transactions_summary_global_by_event_name: All transactions aggregated. There is only a single row in this table.   
events_transactions_summary_by_account_by_event_name: The transactions grouped by username and hostname.   
events_transactions_summary_by_host_by_event_name: The transactions grouped by hostname of the account.   
events_transactions_summary_by_thread_by_event_name: The transactions grouped by thread. Only currently existing threads are included.

# • events_transactions_summary_by_user_by_event_name: The

events grouped by the username part of the account.

Each table includes the columns that the transaction statistics are grouped by and three groups of columns: total, for read-write transactions, and for read-only transactions. For each of these three groups of columns, there is the total number of transactions as well as the total, minimum, average, and maximum latencies. Listing 4-5 shows an example of the data from the events_transactions_summary_global_by_ event_name table.

Listing 4-5. The events_transactions_summary_global_by_event_name table   
-- Connection Processlist ID Thread ID Event ID   
-- 1 60 143 6   
-- Connection 1   
Connection $1>$ SELECT \* FROM performance_schema.events_transactions_summaries_global_by_ event_name\G   
********** 1. row ****** 1. ROW ****** 1. ROW ****** 1. ROW ****** 1. ROW ****** 1. ROW ****** 1. ROW ****** 1. ROW ****** 1. ROW ****** 1. ROW ****** 1. ROW ****** 1. ROW ****** 1. ROW ****** 1. ROW ****** 1. ROW ****** 1. ROW ****** 1. ROW ****** 1. ROW ****** 2   
SUM_TIMER_WAIT:90259064465300000 MIN_TIMER_WAIT:4800000 AVG_TIMER_WAIT:2229444500000 MAX_TIMER_WAIT:62122342944500000 COUNT_READ_WRITE:40483   
SUM_TIMER_READ_WRITE:90230783742700000 MIN_TIMER_READ_WRITE:4800000 AVG_TIMER_READ_WRITE:2228856100000 MAX_TIMER_READ_WRITE:62122342944500000 COUNT_READ_ONLY:2   
SUM_TIMER_READ_ONLY:28280722600000 MIN_TIMER_READ_ONLY:9561820600000 AVG_TIMER_READ_ONLY:14140361300000 MAX_TIMER_READ_ONLY:18718902000000   
1 row in set (O.ooo7 sec)

It may surprise you when you study the output how many transactions there are, particularly read-write transactions. Remember that when querying an InnoDB table, everything is a transaction even if you have not explicitly specified one. So even a simple SELECT statement querying a single row counts as a transaction. Regarding the distribution between read-write and read-only transactions, then the Performance Schema only considers a transaction read-only if you explicitly started it as such

START TRANSACTION READ ONLY;

When InnoDB determines that an autocommitting single-statement transaction can be treated as a read-only transaction, that is still counting toward the read-write statistics in the Performance Schema.

# Summary

In this chapter, the transaction-related tables in the Performance Schema have been introduced, and it has been shown how you can join to other tables. First the three tables with one row per transaction event, events_transactions_current, events_ transactions_history, and events_transactions_history_long, were discussed, and then they were used to join on the statement event tables to obtain the most recent statements executed in a transaction. Finally, the transaction summary tables were covered.

You have now covered the most important resources for monitoring locks and transactions, and it is time to go into detail about locks. First, you will learn about the lock access levels.

# Lock Access Levels

In the first chapter where locks were introduced, there was no mention of how locks work. It would be possible to implement locking in the database by just allowing one query access at a time irrespective of what kind of work will be done. However, this would be woefully inefficient.

Keeping to the analogy of a traffic light, another approach is grant access based on what work will be done. A traffic light grants access to the intersection not just to one car at a time but to all driving in the same direction. Similarly, in a database, you distinguish between shared (read) and exclusive (write) access. The access levels do what their names suggest. A shared lock allows other connections to also get a shared lock. This is the most permissive lock access level. An exclusive lock only allows that one connection to get the lock. A shared lock is also known as a read lock, and an exclusive lock is also known as a write lock.

Note The lock access level is also sometimes called the lock type, but since that can be confused with the lock granularity, which is also sometimes called a type, the term lock access level is used here.

MySQL also has a concept called intention locks which specify the intention of a transaction. An intention lock can be either shared or exclusive.

The rest of this chapter goes into more detail about shared and exclusive locks as well as intention locks.

# Shared Locks

When a thread needs to protect a resource, but it is not going to change the resource, it can use a shared lock to prevent other threads from changing the resource while still allowing them to access the same resource. This is the most commonly used access level.

Whenever a statement selects from a table, MySQL will take a shared lock on the tables involved in the query. For the data locks, it works differently as InnoDB does not in general acquire a shared lock when reading a row. That only happens when a shared lock is explicitly requested, in the SERIALIZABLE transaction isolation level, or it is required by the workflow such as when foreign keys are involved.

You can explicitly request a shared lock on the rows accessed by a query by adding the FOR SHARE or its synonym LOCK IN SHARE MODE as shown in Listing 5-1.

Listing 5-1. Example of obtaining a shared lock   
```sql
-- Connection Processlist ID Thread ID Event ID
-- 1 36 80 6
-- Connection 1
Connection 1> START TRANSACTION;
Query OK, 0 rows affected (0.0003 sec)
Connection 1> SELECT * FROM world.city WHERE ID = 130 FOR SHARE;
+-----------------------------------+
| ID | Name | CountryCode | District | Population |
+-----------------------------------+
| 130 | Sydney | AUS | New South Wales | 3276207 |
+-----------------------------------+
| 1 row in set (0.0047 sec)
Connection 1> SELECT object_type, object_schema, object_name,
lock_type, lock_duration, lock_status
FROM performance_schema_metadata_locks
WHERE OWNER_THREAD_ID = PS_CURRENT_THREAD_ID()
AND OBJECT_schema <> 'performance_schema' \G
********** 1. row *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** ******
object_type: TABLE
object_schema: world
object_name: city
lock_type: SHARED_READ
lock_duration: TRANSACTION 
```

lock_status:GRANTED   
1 row in set (0.0005 sec)   
Connection 1> SELECT engine, object_schema, object_name, lock_type, lock_mode, lock_status FROM performance_schema.data_locks WHERE THREAD_ID $=$ PS_CURRENT_THREAD_ID()\G   
**********1. row*******1. row*******1. row*******1. row*******1. row*******1. row*******1. row*******1. row*******1. row*******1. row*******1. row*******1. row*******1. row*******1. row*******1. row*******1. row*******1. row*******1. row*******1. row*******1. row*******1. row*******1. row*******1. row*******1. row*******1. row*******1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*******1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row******1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****

When querying the metadata_locks table, locks on the Performance Schema tables are excluded as they are for the investigation query itself rather than for the previous query. Here a shared lock is taken on world.city table as well as the record with the primary key (the ID column) equals to 130. That they are shared locks can be seen from the lock_type column in the metadata_locks table which has a value of SHARED_READ and from the S in the lock_mode column of data_locks in the second row. The value of IS for the first row from data_locks means it is a shared intention lock which will be discussed in more detail shortly.

While shared locks do allow other queries also using shared locks to proceed, they do block attempts to get an exclusive lock

# Exclusive Locks

Exclusive locks are the counterpart to shared lock. They ensure that only the thread granted the exclusive lock can access the resource for the duration of the lock. As exclusive locks are used to ensure only one thread is modifying a resource at a time, they are also known as write locks.

Exclusive locks are mostly obtained by data definition language (DDL) statements such as ALTER TABLE and when modifying data using data modification language (DML) statements such as UPDATE and DELETE. As example of obtaining an exclusive lock and the data in the lock tables can be found in Listing 5-2.

Listing 5-2. Example of obtaining exclusive locks   
```txt
-- Connection Processlist ID Thread ID Event ID
-- 1 38 84 6 
```

```txt
-- Connection 1
Connection 1> START TRANSACTION;
Query OK, 0 rows affected (0.0003 sec) 
```

```txt
Connection 1> UPDATE world.city  
SET Population = Population + 1  
WHERE ID = 130;  
Query OK, 1 row affected (0.0028 sec) 
```

```txt
Rows matched: 1 Changed: 1Warnings: 0 
```

```txt
Connection 1> SELECT object_type, object_schema, object_name, lock_type, lock_duration, lock_status FROM performance_schema.meta_data_lockS WHERE OWNER_THREAD_ID = PS_CURRENT_THREAD_ID() AND OBJECT_schema <> 'performance_schema'\G   
********** 1. row   
object_type: TABLE   
object_schema: world   
object_name: city   
lock_type: SHARED_WRITE 
```

```txt
lock_duration: TRANSACTION  
lock_status: GRANTED  
********** 2. row ***  
object_type: TABLE  
object_schema: world  
object_name: country  
lock_type: SHARED_READ  
lock_duration: TRANSACTION  
lock_status: GRANTED  
2 rows in set (0.0008 sec)  
Connection 1> SELECT engine, object_schema, object_name, lock_type, lock_mode, lock_status FROM performance_schema.data_lockS WHERE THREAD_ID = PS_CURRENT_THREAD_ID()\G  
********** 1. row ***  
engine: INNODB  
object_schema: world  
object_name: city  
lock_type: TABLE  
lock_mode: IX  
lock_status: GRANTED  
********** 2. row ***  
engine: INNODB  
object_schema: world  
object_name: city  
lock_type: RECORD  
lock_mode: X,REC_NOT_GAP  
lock_status: GRANTED  
2 rows in set (0.0005 sec)  
mysql> ROLLBACK;  
Query OK, 0 rows affected (0.3218 sec) 
```

Most of the example reflects the example of obtaining shared locks, but there are also some surprises. To start with the data_locks table, it shows an exclusive insert intention (IX) lock on the table and an exclusive (X) record lock. This is as expected.

It becomes more complicated with the metadata_locks table where there are now two table locks, a SHARED_WRITE lock on the city table and a SHARED_READ lock on the country table. How can a lock both be shared and write at the same time, why is the lock on the city table shared when it was modified, and why is there a lock on the country table?

A SHARED_WRITE lock tells that the data is locked for updates but that the metadata lock itself is a shared lock. The reason for this is that the metadata for the table is not modified, so it is safe to allow other concurrent shared access to the table metadata. Remember that the metadata_locks table does not care about the locks held on individual records, so from a metadata perspective, the access to the city table is shared.

The metadata lock on the country table comes from the foreign key on the city table to the country table. The shared lock prevents modifications to the country metadata, such as dropping the column involved in the foreign key, while the transaction is still ongoing. Chapter 10 will go into more detail about the effect of foreign keys on locking.

# Intention Locks

In the two examples this far in this chapter, there have been intention locks. What are those? It is a lock that signals the intention of an InnoDB transaction and can be either shared or exclusive. It can at first seem an unnecessary complication, but the intention locks allow InnoDB to resolve the lock requests in order without blocking compatible operations. The details are beyond the scope of this discussion. The important thing is that you know that the intention locks exist, so when you see them you know where they come from.

Intention locks from a more functional perspective are covered in Chapter 6, and a related concept, insert intention locks are covered with the InnoDB locks in Chapter 7.

# Lock Compatibility

The lock compatibility matrix defines whether two lock requests conflict with each other. The introduction of intention locks makes this a little more complex than saying that shared locks are compatible with each other and exclusive locks are not compatible with any other locks.

Two intention locks are always compatible with each other. This means that even if a transaction has an intention exclusive lock, it will not prevent another transaction to take an intention lock. It will however stop the other transaction from upgrading its intention lock to a full lock. Table 5-1 shows the compatibility between the lock types. Shared locks are denoted S and exclusive locks X. Intention locks are prefixed I, so IS is an intention shared lock and IX is an intention exclusive lock.

Table 5-1. InnoDB lock compatibility   

<table><tr><td></td><td>Exclusive (X)</td><td>Intention</td><td>Exclusive (IX)</td><td>Shared (S)</td><td>Intention Shared (IS)</td></tr><tr><td>Exclusive (X)</td><td>X</td><td>X</td><td></td><td>X</td><td>X</td></tr><tr><td>Intention</td><td>X</td><td>✓</td><td></td><td>X</td><td>✓</td></tr><tr><td>Exclusive (IX)</td><td></td><td></td><td></td><td></td><td></td></tr><tr><td>Shared (S)</td><td>X</td><td>X</td><td></td><td>✓</td><td>✓</td></tr><tr><td>Intention</td><td>X</td><td>✓</td><td></td><td>✓</td><td>✓</td></tr><tr><td>Shared (IS)</td><td></td><td></td><td></td><td></td><td></td></tr></table>

In the table, a checkmark indicates that the two locks are compatible, whereas a cross mark indicates the two locks are conflicting with each other. The only conflicts of intention locks are with the exclusive and shared locks. An exclusive lock conflicts with all other locks including both intention lock types. A shared lock conflicts only with an exclusive lock and an intention exclusive lock.

This does sound fairly simple; however, this only applies to two of the same kind of locks. When you start to include different locks at the InnoDB level, it becomes more complex as will be discussed in Chapter 8 when lock contention is discussed.

This is all handled automatically by MySQL and InnoDB; however, you need to understand these rules when investigating lock issues.

# Summary

This chapter has discussed the MySQL lock access levels. A lock can either be a shared lock, an exclusive lock, an intention shared lock, or an intention exclusive lock.

A shared lock is for read access to a resource and allows multiple threads to access the same resource concurrently. An exclusive lock on the other hand only allows a single thread to access the resource at a time which makes it safe to update the resource.

Intention locks are an InnoDB concept that allows InnoDB to resolve lock requests with less blocked requests as a result. All intention locks are compatible with each other, even intention exclusive locks, but intention exclusive locks block shared locks.

In this as well as the previous chapters, you have also encountered examples of how locks guard different resources such as tables and records. In the next chapter, it is time to learn more about the high-level lock access types such as table and metadata locks.

# High-Level Lock Types

In the previous chapter, you learned about the shared and exclusive access levels. In principle, you can make a locking system that does not consist of anything but one type of lock that can either be shared or exclusive. It would however mean that it would have to work at the instance level and thus be very poorly at allowing concurrent read-write access to the data. In this and the next chapter, you will learn how there are many kinds of locks depending on the resource they protect. While this does make locking much more complex, it does also allow for a much fine grainer locks that leads to support for a higher concurrency.

This chapter discusses the high-level locks in MySQL starting with the user-level locks and going through the various types of locks that are handled at the MySQL level (i.e., above the storage engine). Included are flush locks, metadata locks, explicit and implicit table locks (which are an exception as they are handled by InnoDB), backup locks, and log locks.

# User-Level Locks

User-level locks are an explicit lock type the application can use to protect, for example, a workflow. They are not often used, but they can be useful for some complex tasks where you want to serialize access. All user locks are exclusive locks and are obtained using a name which can be up to 64 characters long.

You manipulate user-level locks with a set of functions:

• GET_LOCK(name, timeout): Obtains a lock by specifying the name of the lock. The second argument is a timeout in seconds; if the lock is not obtained within that time, the function returns 0. If the lock is obtained, the return value is 1. If the timeout is negative, the function will wait indefinitely for the lock to become available.

IS_FREE_LOCK(name): Checks whether the named lock is available or not. The function returns 1 if the lock is available and 0 if it is not available.   
IS_USED_LOCK(name): This is the opposite of the IS_FREE_LOCK() function. The function returns the connection id of the connection holding the lock if the lock is in use (not available) and NULL if it is not in use (available).   
RELEASE_ALL_LOCKS(): Releases all user-level locks held by the connection. The return value is the number of locks released.   
RELEASE_LOCK(name): Releases the lock with the provided name. The return value is 1 if the lock is released, 0 if the lock exists but is not owned by the connection, or NULL if the lock does not exist.

It is possible to obtain multiple locks by invoking GET_LOCK() multiple times. If you do that, be careful to ensure locks are obtained in the same order by all users as otherwise a deadlock can occur. If a deadlock occurs, an ER_USER_LOCK_DEADLOCK error (error code 3058) is returned. An example of this is shown in Listing 6-1.

Listing 6-1. A deadlock for user-level locks   
```sql
-- Connection Processlist ID Thread ID Event ID   
-- 1 322 617 6   
-- 2 323 618 6   
-- Connection 1   
Connection 1> SELECT GET_LOCK('my_lock_1', -1);   
+   
| GET_LOCK('my_lock_1', -1) |   
+   
| 1   
+   
1 row in set (0.0003 sec)   
-- Connection 2   
Connection 2> SELECT GET_LOCK('my_lock_2', -1); 
```

```diff
+  
| GET_LOCK('my_lock_2', -1)  
+  
| 1  
+  
1 row in set (0.0003 sec) 
```

```sql
Connection 2> SELECT GET_LOCK('my_lock_1', -1);  
-- Connection 1  
Connection 1> SELECT GET_LOCK('my_lock_2', -1); 
```

ERROR: 3058: Deadlock found when trying to get user-level lock; try rolling back transaction/releasing locks and restarting lock acquisition.

When Connection 2 attempts to get the my_lock_1 lock, the statement will block until Connection 1 attempts to get the my_lock_2 lock triggering the deadlock. If you obtain multiple locks, you should be prepared to handle deadlocks. Note that for userlevel locks, a deadlock does not trigger a rollback of the transaction.

The granted and pending user-level locks can be found in the performance_schema. metadata_locks table with the OBJECT_TYPE column set to USER LEVEL LOCK as shown in Listing 6-2. The locks listed assume you left the system as it was at the time the deadlock in Listing 6-1 was triggered. Note that some values such as OBJECT_INSTANCE_BEGIN will be different for you and you will have to change the ids for owner_thread_id in the WHERE clause to match yours from Listing 6-1.

Listing 6-2. Listing user-level locks   
-- Investigation #1   
-- Connection 3   
Connection $3>$ SELECT \* FROM performance_schema_metadata_locks WHERE object_type $\equiv$ 'USER LEVEL LOCK' AND owner_thread_id IN (617, 618)\G   
********** 1. row

OBJECT_TYPE: USER LEVEL LOCK

OBJECT_SCHEMA: NULL

OBJECT_NAME: my_lock_1

COLUMN_NAME: NULL

Chapter 6 High-Level Lock Types

OBJECT_INSTANCE_BEGIN: 2124404669104

LOCK_TYPE: EXCLUSIVE

LOCK_DURATION: EXPLICIT

LOCK_STATUS: GRANTED

SOURCE: item_func.cc:5067

OWNER_THREAD_ID: 617

OWNER_EVENT_ID: 8

*************************** 2. row ***************************

OBJECT_TYPE: USER LEVEL LOCK

OBJECT_SCHEMA: NULL

OBJECT_NAME: my_lock_2

COLUMN_NAME: NULL

OBJECT_INSTANCE_BEGIN: 2124463901664

LOCK_TYPE: EXCLUSIVE

LOCK_DURATION: EXPLICIT

LOCK_STATUS: GRANTED

SOURCE: item_func.cc:5067

OWNER_THREAD_ID: 618

OWNER_EVENT_ID: 8

*************************** 3. row ***************************

OBJECT_TYPE: USER LEVEL LOCK

OBJECT_SCHEMA: NULL

OBJECT_NAME: my_lock_1

COLUMN_NAME: NULL

OBJECT_INSTANCE_BEGIN: 2124463901088

LOCK_TYPE: EXCLUSIVE

LOCK_DURATION: EXPLICIT

LOCK_STATUS: PENDING

SOURCE: item_func.cc:5067

OWNER_THREAD_ID: 618

OWNER_EVENT_ID: 9

3 rows in set (0.0015 sec)

The OBJECT_TYPE for user-level locks is USER LEVEL LOCK, and the lock duration is EXPLICIT as it is up to the user or application to release the lock again. In row 1, the connection with Performance Schema thread id 617 has been granted the my_lock_1

lock, and in row 3 thread id 618 is waiting (pending) for it to be granted. Thread id 618 also has a granted lock which is included in row 2. Once you are done with the investigation, remember to release the locks, for example, executing SELECT RELEASE_ ALL_LOCKS() first in connection 1 and then in connection 2 (this happens automatically when exiting the workload using the MySQL Shell concurrency_book module).

The next level of locks involves non-data table-level locks. The first of these that will be discussed is the flush lock.

# Flush Locks

A flush lock will be familiar to most who have been involved in taking backups. It is taken when you use the FLUSH TABLES statement and last for the duration of the statement unless you add WITH READ LOCK in which case a shared (read) lock is held until the lock is explicitly released. An implicit table flush is also triggered at the end of the ANALYZE TABLE statement. The flush lock is a table-level lock. The read lock taken with FLUSH TABLES WITH READ LOCK is discussed later under explicit table locks.

A common cause of lock issues for the flush lock is long-running queries. A FLUSH TABLES statement cannot flush a table as long as there is a query that has the table open. This means that if you execute a FLUSH TABLES statement while there is a long-running query using one or more of the tables being flushed, then the FLUSH TABLES statement will block all other statements needing any of those tables until the lock situation has been resolved.

Flush locks are subject to the lock_wait_timeout setting. If it takes more than lock_wait_timeout seconds to obtain the lock, MySQL will abandon the lock. The same applies if the FLUSH TABLES statement is killed. However, due to the internals of MySQL, a lower-level lock called the table definition cache (TDC) version lock cannot always be released until the long-running query completes.1 That means that the only way to be sure the lock problem is resolved is to kill the long-running query, but be aware that if the query has changed many rows, it may take a long time to roll back the query.

When there is lock contention around the flush lock, both the FLUSH TABLES statement and the queries started subsequently will have the state set to “Waiting for table flush.” Listing 6-3 shows an example of this involving three queries. If you are reproducing the scenario yourself (as opposed to using the MySQL Shell concurrency_book module),

then you can change the argument to SLEEP() in connection 1 to give yourself more time to complete the example.

Listing 6-3. Example of waiting for a flush lock   
```txt
-- Connection Processlist ID Thread ID Event ID
-- 1 375 691 6
-- 2 376 692 6
-- 3 377 693 6
-- 4 378 694 6
```

```sql
-- Connection 1
Connection 1> SELECT city.*, SLEEP(3) FROM world.city WHERE ID = 130; 
```

```sql
-- Connection 2
Connection 2> FLUSH TABLES world.city; 
```

```sql
-- Connection 3
Connection 3> SELECT * FROM world.city WHERE ID = 201; 
```

```txt
-- Connection 4 -- Query sys.session for the three threads involved 
```

```sql
Connection 4> SELECT thd_id, conn_id, state, current_statement FROM sys.session WHERE current_statement IS NOT NULL AND thd_id IN (691, 692, 693) ORDER BY thd_id\G 
```

```txt
********** 1. row ***  
thd_id: 691  
conn_id: 375  
state: User sleep 
```

```txt
current_statement: SELECT city.*, SLEEP(3) FROM world.city WHERE ID = 130  
********** 2. row ***  
thd_id: 692  
conn_id: 376 
```

state: Waiting for table flush  
current_statement: FLUSH TABLES world.city  
********** 3. ROW *** ****** ******  
thd_id: 693  
conn_id: 377  
state: Waiting for table flush  
current_statement: SELECT * FROM world.city WHERE ID = 201  
3 rows in set (0.0586 sec)

The example uses the sys.session view; similar results can be obtained using performance_schema.threads and SHOW PROCESSLIST. In order to reduce the output to only include the queries of relevance for the flush lock discussion, the WHERE clause is set to only include the threads ids of the first three connections.

The connection with conn_ $\dot { 1 } \dot { 1 } \dot { 1 } = 3 7 5$ is executing a slow query that uses the world. city table (a SLEEP(3) was used to ensure it took enough time to execute the statements for the other connections). In the meantime, conn_ $\dot { 1 } \dot { 1 } \dot { 1 } = 3 7 6$ executed a FLUSH TABLES statement for the world.city table. Because the first query still has the table open (it is released once the query completes), the FLUSH TABLES statement ends up waiting for the table flush lock. Finally, conn_id = 377 attempts to query the table and thus must wait for the FLUSH TABLES statement.

Another non-data table lock is a metadata lock.

# Metadata Locks

Metadata locks are one of the newer lock types in MySQL. They were introduced in MySQL 5.5, and their purpose is to protect the schema, so it does not get changed while queries or transactions rely on the schema to be unchanged. Metadata locks work at the table level, but they should be considered as an independent lock type to table locks as they do not protect the data in the tables.

SELECT statements and DML queries take a shared metadata lock, whereas DDL statements take an exclusive lock. A connection takes a metadata lock on a table when the table is first used and keeps the lock until the end of the transaction. While the metadata lock is held, no other connection is allowed to change the schema definition of the table. However, other connections that execute SELECT statements and DML statements are not restricted. Usually the biggest gotcha with respect to metadata locks is long-running transactions, possibly being idle, preventing DDL statements from starting their work.

If you encounter a conflict around a metadata lock, you will see the query state in the process list set to “Waiting for table metadata lock.” An example of this including queries to setup is shown in Listing 6-4.

Listing 6-4. Example of waiting for table metadata lock   
```txt
-- Connection Processlist ID Thread ID Event ID
-- 1 428 768 6
-- 2 429 769 6 
```

```sql
-- Connection 1  
Connection 1> START TRANSACTION;  
Query OK, 0 rows affected (0.0003 sec) 
```

```txt
Connection 1> SELECT * FROM world.city WHERE ID = 130\G
********** 1. row *** 
```

```txt
ID: 130  
Name: Sydney  
CountryCode: AUS  
District: New South Wales  
Population: 3276207  
1 row in set (0.0006 sec)  
-- Connection 2  
Connection 2> OPTIMIZE TABLE world.city; 
```

Connection 2 blocks, and while you are in this situation, you can query sys.session or similar as shown in Listing 6-5.

Listing 6-5. sys.session for the connections involved in the metadata lock   
```sql
-- Investigation #1  
-- Connection 3  
Connection 3> SELECT thd_id, conn_id, state, current_statement, statement_longevity, last_statement, trx_state 
```

```txt
FROM sys.session WHERE conn_id IN (428, 429) ORDER BY conn_id\G   
**********1. row**********   
thd_id: 768 conn_id: 428 state: NULL   
current_statement: SELECT \* FROM world.city WHERE ID = 130 statement-latency: NULL last_statement: SELECT \* FROM world.city WHERE ID = 130trx_state: ACTIVE   
**********2. row********** thd_id: 769 conn_id: 429 state: Waiting for table metadata lock   
current_statement: OPTIMIZE TABLE world.city   
statement-latency: 26.62 s last_statement: NULL trx_state: COMMITTED   
2 rows in set (0.0607 sec) 
```

In this example, the connection with conn_id = 428 has an ongoing transaction and in the previous statement queried the world.city table (the current statement in this case is the same as it is not cleared until the next statement is executed). While the transaction is still active, conn_ $\dot { 1 } \dot { 1 } \dot { 1 } = 4 2 9$ has executed an OPTIMIZE TABLE statement which is now waiting for the metadata lock. (Yes, OPTIMIZE TABLE does not change the schema definition, but as a DDL statement, it is still affected by the metadata lock.) Since MySQL does not have transactional DDL statements, the transaction state for conn $\dot { \bf 1 } \dot { \bf d } = 4 2 9$ shows up as committed.

It is convenient when it is the current or last statement that is the cause of the metadata lock. In more general cases, you can use the performance_schema.metadata_ locks table with the OBJECT_TYPE column set to TABLE to find granted and pending metadata locks. Listing 6-6 shows an example of granted and pending metadata locks using the same setup as in the previous example. Chapter 14 goes into more detail about investigating metadata locks.

Listing 6-6. Example of metadata locks   
-- Investigation #2   
Connection 3> SELECT object_type, object_schema, object_name, lock_type, lock_duration, lock_status, owner_thread_id FROM performance_schema_metadata_locks WHERE owner_thread_id IN (768, 769) AND object_type $=$ 'TABLE'\G   
**********1. row*******1. row*******1. row*******1. row*******1. row*******1. row*******1. row*******1. row*******1. row*******1. row*******1. row*******1. row*******1. row*******1. row*******1. row*******1. row*******1. row*******1. row*******1. row*******1. row*******1. row*******1. row*******1. row*******1. row*******1. row*******1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*******1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row******1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row*****1. row******

In the example, thread id 768 (the same as conn_id = 428 from the sys.session output) owns a shared read lock on the world.city table due to an ongoing transaction, and thread id 769 is waiting for a lock as it is trying to execute a DDL statement on the table.

When you are done, make sure you roll back or commit the transaction in Connection 1, so the OPTIMIZE TABLE can complete:

```sql
-- Connection 1  
Connection 1> ROLLBACK;  
Query OK, 0 rows affected (0.0004 sec) 
```

A special case of metadata locks are locks taken explicitly with the LOCK TABLES statement.

# Explicit Table Locks

Explicit table locks are taken with the LOCK TABLES and the FLUSH TABLES WITH READ LOCK statements. With the LOCK TABLES statement, it is possible to take shared or exclusive locks; FLUSH TABLES WITH READ LOCK always takes a shared lock. The tables are locked, until they are explicitly released with the UNLOCK TABLES statement. When FLUSH TABLES WITH READ LOCK is executed without listing any tables, the global read lock (i.e., affecting all tables) is taken. While these locks also protect the data, they are considered as metadata locks in MySQL.

Explicit table locks, other than FLUSH TABLES WITH READ LOCK in connection with backups, are not often used with InnoDB as InnoDB’s sophisticated lock features are in most cases superior to handling locks yourself. However, if you really need to lock the entire tables, explicit locks can be useful as they are very cheap for MySQL to check.

An example of a connection taking an explicit read lock on the world.country and world.countrylanguage tables and a write lock on the world.city table is shown in Listing 6-7.

Listing 6-7. Using explicit table locks   
```sql
-- Connection Processlist ID Thread ID Event ID  
-- 1 432 772 6  
-- Connection 1  
Connection 1> LOCK TABLES world.cOUNTRY READ, world.cOUNTRY language READ, world.city WRITE;  
Query OK, 0 rows affected (0.0029 sec) 
```

When you take explicit locks, you are only allowed to use the tables you have locked and in accordance with the requested locks. This means you will get an error if you take a read lock and attempt to write to the table (ER_TABLE_NOT_LOCKED_FOR_WRITE) or if you try to use a table you did not take a lock for (ER_TABLE_NOT_LOCKED), for example (continuation of Listing 6-7)

Chapter 6 High-Level Lock Types

Connection 1> UPDATE world.cOUNTRY SET Population $=$ Population $+1$ WHERE Code $=$ 'AUS';

ERROR: 1099: Table 'country' was locked with a READ lock and can't be updated

Connection 1> SELECT * FROM sakila.film WHERE film_id = 1;

ERROR: 1100: Table 'film' was not locked with LOCK TABLES

Since explicit locks are considered metadata locks, the symptoms and information in the performance_schema.metadata_locks table are the same as for implicit metadata locks, and you also unlock the tables using the UNLOCK TABLES statement:

Connection 1> UNLOCK TABLES; Query OK, 0 rows affected (0.0006 sec)

Another table-level lock but handled implicitly is plainly called a table lock.

# Implicit Table Locks

MySQL takes implicit table locks when a table is queried. Table locks do not play a large role for InnoDB tables except for flush, metadata, and explicit locks as InnoDB uses record locks to allow concurrent access to a table as long as the transactions do not modify the same rows (roughly speaking – as the next chapter shows – there is more to it than that).

InnoDB does however work with the concept of intention locks at the table level. Since you are likely to encounter those when investigating lock issues, it is worth familiarizing yourself with them. As mentioned in the discussion of lock access levels, intention locks mark what the intention of the transaction is.

For locks taken by transactions, first, an intention lock is taken, and then it may if needed be upgraded. This is unlike an explicit LOCK TABLES that does not change. To get a shared lock, the transaction first takes an intention shared lock and then the shared lock. Similarly, for an exclusive lock, an intention exclusive lock is first taken. Some examples of intention locks are as follows:

• A SELECT ... FOR SHARE statement takes an intention shared lock on the tables queried. The SELECT ... LOCK IN SHARE MODE syntax is a synonym.   
A SELECT ... FOR UPDATE statement takes an intention exclusive lock on the tables queried.   
A DML statement (not including SELECT) takes an intention exclusive lock on the modified tables. If a foreign key column is modified, an intention shared lock is taken on the parent table.

The table-level locks can be found in the performance_schema.data_locks table with the LOCK_TYPE column set to TABLE. Listing 6-8 shows an example of an intention shared lock.

Listing 6-8. Example of an InnoDB intention shared lock   
```txt
-- Connection Processlist ID Thread ID Event ID
-- 1 446 796 6
-- 2 447 797 6 
```

```sql
-- Connection 1  
Connection 1> START TRANSACTION;  
Query OK, 0 rows affected (0.0003 sec) 
```

```txt
Connection 1> SELECT * FROM world.city WHERE ID = 130 FOR SHARE\G 
```

```txt
********** 1. row********** ID: 130 
```

```txt
Name: Sydney  
CountryCode: AUS  
District: New South Wales  
Population: 3276207  
1 row in set (0.0010 sec)  
-- Connection 2 
```

```sql
Connection 2> SELECT engine, thread_id, object_schema, object_name, lock_type, lock_mode, lock_status, lock_data FROM performance_schema.data_lockS WHERE lock_type = 'TABLE' AND thread_id = 796\G 
```

```txt
********** 1. row *** 
```

```txt
engine: INNODB thread_id: 796 object_schema: world object_name: city lock_type: TABLE lock_mode: IS lock_status: GRANTED 
```

```txt
lock_data: NULL  
1 row in set (0.0011 sec)  
-- Connection 1  
Connection 1> ROLLBACK;  
Query OK, 0 rows affected (0.0004 sec) 
```

This shows an intention shared lock on the world.city table. Notice that the engine is set to INNODB and that lock_data is NULL.

# Backup Locks

The backup lock is an instance-level lock; that is, it affects the system as a whole. It is a new lock introduced in MySQL 8. The backup lock prevents statements that can make a backup inconsistent while still allowing other statements to be executed concurrently with the backup. Currently the primary user of the backup lock is MySQL Enterprise Backup which uses it together with the log lock to avoid executing FLUSH TABLES WITH READ LOCK for InnoDB tables. The statements that are blocked include

• Statements that create, rename, or remove files. These include CREATE TABLE, CREATE TABLESPACE, RENAME TABLE, and DROP TABLE statements.

Account management statements such as CREATE USER, ALTER USER, DROP USER, and GRANT.   
DDL statements that do not log their changes to the redo log. These, for example, include adding an index.

A backup lock is created with the LOCK INSTANCE FOR BACKUP statement, and the lock is released with the UNLOCK INSTANCE statement. It requires the BACKUP_ADMIN privileges to execute LOCK INSTANCE FOR BACKUP. An example of obtaining the backup lock and releasing it again is

```txt
mysql> LOCKINSTANCE FOR BACKUP;  
Query OK, 0 rows affected (0.0002 sec)  
mysql> UNLOCKINSTANCE;  
Query OK, 0 rows affected (0.0003 sec) 
```

Note A t the time of writing, taking a backup lock and releasing it is not allowed when using the X Protocol (connecting through the port specified with mysqlx_ port or the socket specified with mysqlx_socket). Attempting to do so returns an ER_PLUGGABLE_PROTOCOL_COMMAND_NOT_SUPPORTED error: ERROR: 3130: Command not supported by pluggable protocols.

Additionally, statements that conflict with the backup lock also take the backup lock. Since DDL statements sometimes consist of several steps, for example, rebuilding a table in a new file and renaming the file, the backup lock can be released between the steps to avoid blocking LOCK INSTANCE FOR BACKUP for longer than necessary.

Backup locks can be found in the performance_schema.metadata_locks table with the OBJECT_TYPE column set to BACKUP LOCK. Listing 6-9 shows an example of a query waiting for a backup lock held by LOCK INSTANCE FOR BACKUP.

# Listing 6-9. Example of a conflict for the backup lock

<table><tr><td>-- Connection</td><td>Processlist ID</td><td>Thread ID</td><td>Event ID</td></tr><tr><td>--</td><td>1</td><td>484</td><td>851</td></tr><tr><td>--</td><td>2</td><td>485</td><td>852</td></tr><tr><td>--</td><td>3</td><td>486</td><td>853</td></tr></table>

Chapter 6 High-Level Lock Types   
```sql
-- Connection 1   
Connection 1> LOCK instances FOR BACKUP;   
Query OK, 0 rows affected (0.0004 sec)   
-- Connection 2   
Connection 2> OPTIMIZE TABLE world.city;   
-- Connection 3   
Connection 3> SELECT object_type, object_schema, object_name, lock_type, lock_duration, lock_status, owner_thread_id FROM performance_schema_metadata_locks WHERE object_type = 'BACKUP LOCK' AND owner_thread_id IN (851, 852)\G   
********** 1. row *** 
```

```txt
object_type: BACKUP LOCK  
object_schema: NULL  
object_name: NULL  
lock_type: SHARED  
lock_duration: EXPLICIT  
lock_status: GRANTED  
owner_thread_id: 851  
********** 2. row *** *** *** *** *** 
```

```txt
object_type: BACKUP LOCK  
object_schema: NULL  
object_name: NULL  
lock_type: INTENTION_EXCLUSIVE  
lock_duration: TRANSACTION  
lock_status: PENDING  
owner_thread_id: 852  
2 rows in set (0.0007 sec)  
-- Connection 1  
Connection 1> UNLOCK instances;  
Query OK, 0 rows affected (0.0003 sec) 
```

In the example, the connection with thread id 851 owns the backup lock, whereas the connection with thread id 852 is waiting for it. Notice that LOCK INSTANCE FOR BACKUP holds a shared lock, whereas the DDL statement requests an intention exclusive lock.

Related to the backup lock is the log lock which has also been introduced to reduce locking during backups.

# Log Locks

When you create a backup, you typically want to include information about the log positions and GTID set the backup is consistent with. In MySQL 5.7 and earlier, you needed the global read lock while obtaining this information. In MySQL 8, the log lock was introduced to allow you to read information such as the executed global transaction identifiers (GTIDs), the binary log position, and the log sequence number (LSN) for InnoDB without taking a global read lock.

The log lock prevents operations that make changes to log-related information. In practice this means commits, FLUSH LOGS, and similar. The log lock is taken implicitly by querying the performance_schema.log_status table. It requires the BACKUP_ADMIN privilege to access the table. Listing 6-10 shows an example output of the log_status table.

Listing 6-10. Example output of the log_status table   
-- Connection Processlist ID Thread ID Event ID   
-- 1 490 857 6   
-- Connection 1   
Connection $1>$ SELECT \* FROM performance_schema.log_status\G   
********** 1. row*******   
SERVER_UID: fcbb7afc-bdde-11ea-b95f-ace2d35785be LOCAL: {"gtid_executed": "d2549c41-86ca-11ea-9dc7- ace2d35785be:1-351", "binary_log_file": "binlog.000002", "binary_log_position": 39348} REPLICATION: {"channels": ["channel_name": "", "relay_log_file": "relay-bin.000002", "relay_log_position": 39588}]

Chapter 6 High-Level Lock Types

STORAGE_ENGINES: {"InnoDB": {"LSN": 2073604726, "LSN_checkpoint": 2073604726}} 1 row in set (0.0012 sec)

The information available depends on the configuration of the instance, and the values depend on the usage.

That concludes the review of the main high-lock types in MySQL.

# Summary

In this chapter the high-level lock types have been discussed. These are mostly independent of the storage engine used and include a range of locks from user-level and instance-level locks to table and metadata locks.

The user-level locks can be used to protect workflows in the application and are the most generic lock type. The flush locks are experienced when tables are flushed and can cause hard to diagnose issues due to the low-level table definition cache (TDC) version lock. The metadata locks protect the metadata of schema objects such as the column definitions for a table.

There are both explicit table locks and implicit table locks of which the implicit locks are the most common when working with InnoDB tables. The implicit locks also include the intention locks.

At the instance level, there are two lock types that were developed with backups in mind. The backup locks protect against changes that will make a backup inconsistent such as changes to users and privileges and certain schema changes. The log lock is an implicit lock taken when querying the performance_schema.log_status to ensure log-related status values can be obtained in a consistent way with minimal locking.

In addition to the high-level locks, InnoDB has its own set of locks working at the record level that will be discussed in the next chapter.

# InnoDB Locks

The locks studied in the previous chapter were all, apart from the InnoDB intention locks, general for MySQL. InnoDB has its own sophisticated locking system that allows for a highly concurrent access to the data. In online transaction processing (OLTP) workloads, benchmarks show that depending on the workload, InnoDB handles up to over 100 concurrent queries well.1 This is not only related to the record-level locks but also low-level semaphores, and the latter is an area of ongoing improvement which is the main reason that newer versions of MySQL handle concurrency better than old versions.

Tip Newer versions of MySQL support higher degrees of concurrent query execution than older versions. Latest in 8.0.21, the lock system mutex was sharded to reduce contention on high-concurrency systems.

In this chapter, first, the InnoDB record locks and next-key locks will be discussed followed by gap locks and predicate locks. The last of the data-level locks that are covered is auto-increment locks which are also important to maintain a good performance at high concurrency inserts. The final topic of the chapter is semaphores.

# Record Locks and Next-Key Locks

Record locks are often called row locks; however, it is more than just locks on rows as it also includes index and gap locks. Related are next-key locks. A next-key lock is the combination of a record lock and a gap lock on the gap before the record. A next-key lock is actually the default lock type in InnoDB, and thus you will just see it as S (shared) and X (exclusive) in the lock outputs.

Record and next-key locks are typically the locks that are meant when talking about InnoDB locks. They are fine-grained locks that aim at just locking the least amount of data while still ensuring the data integrity.

A record or next-key lock can be shared or exclusive and affect just the rows and indexes accessed by the transaction. The duration of exclusive locks is usually the transaction with exceptions, for example, delete-marked records used for uniqueness checks in INSERT INTO ... ON DUPLICATE KEY and REPLACE statements. For shared locks, the duration can depend on the transaction isolation level as discussed in Chapters 9 and 12.

Record and next-key locks can be found using the performance_schema.data_locks table. Listing 7-1 shows an example of the locks from updating rows in the world.city table using the secondary index CountryCode.

Listing 7-1. Example of InnoDB record locks   
```txt
-- Connection Processlist ID Thread ID Event ID
-- 1 544 919 6
-- 2 545 920 6 
```

```sql
-- Connection 1  
Connection 1> START TRANSACTION;  
Query OK, 0 rows affected (0.0002 sec) 
```

```sql
Connection 1> UPDATE world.city  
SET Population = Population + 1  
WHERE CountryCode = 'LUX';  
Query OK, 1 row affected (0.0008 sec) 
```

```txt
Rows matched: 1 Changed: 1Warnings: 0 
```

-- Connection 2   
Connection $2>$ SELECT thread_id, event_id, object_schema, object_name, index_name, lock_type, lock_mode, lock_status, lock_data FROM performance_schema.data_locks WHERE thread_id $\equiv$ 919\G

*************************** 1. row ***************************

```yaml
thread_id: 919  
event_id: 10  
object_schema: world  
object_name: city  
index_name: NULL  
lock_type: TABLE  
lock_mode: IX  
lock_status: GRANTED  
lock_data: NULL  
********** 2. row ***  
thread_id: 919  
event_id: 10  
object_schema: world  
object_name: city  
index_name: CountryCode  
lock_type: RECORD  
lock_mode: X  
lock_status: GRANTED  
lock_data: 'LUX', 2452  
********** 3. row ***  
thread_id: 919  
event_id: 10  
object_schema: world  
object_name: city  
index_name: PRIMARY  
lock_type: RECORD  
lock_mode: X,REC_NOT_GAP  
lock_status: GRANTED  
lock_data: 2452  
********** 4. row ***  
thread_id: 919  
event_id: 10  
object_schema: world  
object_name: city 
```

Chapter 7 Inn oDB Locks   
```txt
index_name: CountryCode lock_type: RECORD lock_mode: X,GAP lock_status: GRANTED lock_data: 'LVA', 2434 4 rows in set (0.0014 sec) -- Connection 1 Connection 1> ROLLBACK; Query OK, 0 rows affected (0.1702 sec) 
```

The first row is the intention exclusive table lock that has already been discussed. The second row is a next-key lock on the CountryCode index for the value (‘LUX’, 2452) where ‘LUX’ is the country code used in the WHERE clause and 2452 is the primary key id added to the nonunique secondary index. The city with $I { \mathsf { D } } \ = \ 2 4 5 2$ is the only city matching the WHERE clause, and the primary key record (the row itself) is shown in the third row of the output. The lock mode is X,REC_NOT_GAP which means it is an exclusive lock on the record but not on the gap.

What is a gap? An example is shown in the fourth row of the output. Gap locks are so important that the discussion of the gap lock is split out into its own.

# Gap Locks

A gap lock protects the space between two records. This can be in the row through the clustered index or in a secondary index. Before the first record in an index page and after the last in the page, there are pseudo-records called the infimum record and supremum record, respectively. Gap locks are often the lock type causing the most confusion. Experience from studying lock issues is the best way to become familiar with them.

Consider the query from the previous example:

```sql
UPDATE world.city  
SET Population = Population + 1  
WHERE CountryCode = 'LUX'; 
```

This query changes the population of all cities with CountryCode $=$ 'LUX'. What happens if a new city is inserted between the update and the commit of the transaction? If the UPDATE and INSERT statements commit in the same order they are executed, all is

as such fine. However, if you commit the changes in the opposite order, then the result is inconsistent as it would be expected the inserted row would also have been updated.

This is where the gap lock comes into play. It guards the space where new records (including records moved from a different position) would be inserted, so it is not changed until the transaction holding the gap lock is completed. If you look at the fourth row in the output from the example in Listing 7-1, you can see an example of a gap lock:

```asm
********** 4. row******************  
thread_id: 919  
event_id: 10  
object_schema: world  
object_name: city  
index_name: CountryCode  
lock_type: RECORD  
lock_mode: X,GAP  
lock_status: GRANTED  
lock_data: 'LVA', 2434  
4 rows in set (0.0014 sec) 
```

This is an exclusive gap lock on the CountryCode index for the value (‘LVA’, 2434). Since the query requested to update all rows with the CountryCode set to “LUX,” the gap lock ensures that no new rows are inserted for the “LUX” country code. The country code “LVA” is the next value in the CountryCode index, so the gap between “LUX” and “LVA” is protected with an exclusive lock. On the other hand, it is still possible to insert new cities with CountryCode $=$ 'LVA'. In some places, this is referred to as a “gap before record” which makes it easier to understand how the gap lock works.

One peculiarity of gap locks is that gap locks do not conflict with another gap lock even if both are exclusive. The purpose of gap locks is not to prevent access to the gap but exclusively to prevent inserting data into the gap. When discussing insert intention locks, you will see how a gap lock blocks the insert.

Gap locks are taken to a much less degree when you use the READ COMMITTED transaction isolation level rather than REPEATABLE READ or SERIALIZABLE.

Related to gap locks are predicate locks.

# Predicate and Page Locks

A predicate lock is similar to a gap lock but applies to spatial indexes where an absolute ordering cannot be made and thus a gap lock does not make sense. Instead of a gap lock, for spatial indexes in the REPEATABLE READ and SERIALIZABLE transaction isolation levels, InnoDB creates a predicate lock on the minimum bounding rectangle (MBR) used for the query or for entire pages. This will allow consistent reads by preventing changes to the data within the minimum bounding rectangle or pages.

When querying the performance_schema.data_locks table, predicate locks will have either PREDICATE or PRDT_PAGE with the latter being a page lock.

As an example of predicate locks, consider the address table in the sakila database. This has the column location which is of the geometry data type with the Spatial Reference System Identifier (SRID) set to 0. (An SRID is required in MySQL 8 to have a spatial index.) The index on the location column is named idx_location. Listing 7-2 shows how a predicate lock is taken when updating one of the addresses.

Listing 7-2. Example of predicate/page locks   
```txt
-- Connection Processlist ID Thread ID Event ID
-- 1 562 954 6
-- 2 563 955 6 
```

```sql
-- Connection 1  
Connection 1> START TRANSACTION;  
Query OK, 0 rows affected (0.0002 sec) 
```

```txt
Connection 1> UPDATE sakila.address
SET address = '42Concurrency Boulevard',
district = 'Punjab',
city_id = 208,
postal_code = 40509,
location = ST_GeomFromText('POINT(75.91 31.53)', 0)
WHERE address_id = 372;
Query OK, 1 row affected (0.0008 sec) 
```

```txt
Rows matched: 1 Changed: 1Warnings: 0   
-- Connection 2   
Connection 2> SELECT engine_lock_id, thread_id, event_id, object_schema, object_name, index_name, lock_type, lock_mode, lock_status, lock_data FROM performance_schema.data_locks WHERE thread_id = 954 AND index_name = 'idx_location'\G   
********** 1. row**********   
engine_lock_id: 2123429833312:1074:12:0:2123393008216 thread_id: 954 event_id: 10 object_schema: sakila object_name: address index_name: idx_location lock_type: RECORD lock_mode: S,PRDT_PAGE lock_status: GRANTED lock_data: infimum pseudo-record   
********** 2. row**********   
engine_lock_id: 2123429833312:1074:13:0:2123393008560 thread_id: 954 event_id: 10 object_schema: sakila object_name: address index_name: idx_location lock_type: RECORD lock_mode: S,PRDT_PAGE lock_status: GRANTED lock_data: infimum pseudo-record   
2 rows in set (0.0006 sec)   
-- Connection 1   
Connection 1> ROLLBACK;   
Query OK, O rows affected (0.0435 sec) 
```

The important part of the update is that the location column is changed. In the output from the data_locks table, it can be seen a predicate page lock was taken.

One final lock type related to records that you should know is insert intention locks.

# Insert Intention Locks

Remember that for table locks, InnoDB has intention locks for whether the transaction will use the table in a shared or exclusive manner. Similarly, InnoDB has insert intention locks at the record level. InnoDB uses these locks – as the name suggests – with INSERT statements to signal the intention to other transactions. As such, the lock is on a yet to be created record (so it is a gap lock) rather than on an existing record. The use of insert intention locks can help increase the concurrency that inserts can be performed at.

You are not very likely to see insert intention locks in lock outputs unless an INSERT statement is waiting for a lock to be granted. You can force a situation where this happens by creating a gap lock in another transaction that will prevent the INSERT statement from completing. The example in Listing 7-3 creates a gap lock in Connection 1 and then in Connection 2 attempts to insert a row which conflicts with the gap lock. Finally, in a third connection, the lock information is retrieved.

# Listing 7-3. Example of an insert intention lock

-- Connection Processlist ID Thread ID Event ID   
-- 1 577 972 6   
-- 2 578 973 6   
-- 3 579 974 6   
-- Connection 1   
Connection $1>$ START TRANSACTION;   
Query OK,0 rows affected (0.0003 sec)   
Connection $1>$ SELECT \* FROM world.city WHERE ID >4079 FOR UPDATE\G   
O rows in set (0.0007 sec)

-- Connection 2

Connection 2> START TRANSACTION;

Query OK, 0 rows affected (0.0002 sec)

Connection 2> INSERT INTO world.city

VALUES (4080, 'Darwin', 'AUS',

'Northern Territory', 146000);

-- Connection 3

Connection 3> SELECT thread_id, event_id,

object_schema, object_name, index_name,

lock_type, lock_mode, lock_status, lock_data

FROM performance_schema.data_locks

WHERE thread_id IN (972, 973)

AND object_name $=$ 'city'

AND index_name $=$ 'PRIMARY'\G

*************************** 1. row ***************************

thread_id: 972

event_id: 10

object_schema: world

object_name: city

index_name: PRIMARY

lock_type: RECORD

lock_mode: X

lock_status: GRANTED

lock_data: supremum pseudo-record

*************************** 2. row ***************************

thread_id: 973

event_id: 10

object_schema: world

object_name: city

index_name: PRIMARY

lock_type: RECORD

lock_mode: X,INSERT_INTENTION

lock_status: WAITING

lock_data: supremum pseudo-record

2 rows in set (0.0007 sec)

# Chapter 7 Inn oDB Locks

```txt
-- Connection 1  
Connection 1> ROLLBACK;  
Query OK, 0 rows affected (0.0003 sec)  
-- Connection 2  
Connection 2> ROLLBACK;  
Query OK, 0 rows affected (0.3035 sec) 
```

Notice that for the RECORD lock, the lock mode includes INSERT_INTENTION – the insert intention lock. In this case, the data locked is the supremum pseudo-record, but that can also be the value of the primary key depending on the situation. If you recall the next-key lock discussion, then X means a next-key lock, but this is a special case as the lock is on the supremum pseudo-record, and it is not possible to lock that, so effectively it is just a gap lock on the gap before the supremum pseudo-record.

Another lock that you need to be aware of when inserting data is the auto-increment lock.

# Auto-Increment Locks

When you insert data into a table that has an auto-increment counter, it is necessary to protect the counter so two transactions are guaranteed to get unique values. If you use statement-based logging to the binary log, there are further restrictions as the autoincrement value is recreated for all rows except the first when the statement is replayed.

InnoDB supports three lock modes, so you can adjust the amount of locking according to your needs. You choose the lock mode with the innodb_autoinc_lock_mode option which takes the values 0, 1, and 2 with 2 being the default in MySQL 8. It requires a restart of MySQL to change the value. The meaning of the values is summarized in Table 7-1.

Table 7-1. Supported values for the innodb_autoinc_lock_mode option   

<table><tr><td>Value</td><td>Mode</td><td>Description</td></tr><tr><td>0</td><td>Traditional</td><td>The locking behavior of MySQL 5.0 and earlier. The lock is held until the end of the statement, so values are assigned in repeatable and consecutive order.</td></tr><tr><td>1</td><td>Consecutive</td><td>For the INSERT statement where the number of rows is known at the start of the query, the required number of auto-increment values is assigned under a lightweight mutex, and the auto-increment lock is avoided. For statements where the number of rows is not known, the auto-increment lock is taken and held to the end of the statement. This was the default in MySQL 5.7 and earlier.</td></tr><tr><td>2</td><td>Interleaved</td><td>The auto-increment lock is never taken, and the auto-increment values for concurrent inserts may be interleaved. This mode is only safe when binary logging is disabled or binlog_format is set to ROW. It is the default value in MySQL 8.</td></tr></table>

The higher the value of innodb_autoinc_lock_mode, the less locking. The price to pay for that is increased number of gaps in the sequence of auto-increment values and for innodb_autoinc_lock_mode = 2 the possibility of interleaved values. Unless you cannot use row-based binary logging or have special needs for consecutive autoincrement values, it is recommended to use the value of 2.

That concludes the discussion of data-level locks, but when discussing MySQL concurrency, there is one important topic left: mutexes and rw-lock semaphores.

# Mutexes and RW-Lock Semaphores

Inside the MySQL source code, it is necessary to protect code paths. An example is to protect the code that modifies the content of the buffer pool to avoid two threads modifying the buffer pool content at the same time and thus potentially causing conflicting changes. In some way, you can compare mutexes to the user-level locks, except the former is for the MySQL code paths and the latter for the application code paths using MySQL.

Note InnoDB uses the terms mutex and semaphore somewhat interchangeably. For example, the SEMAPHORES section in the InnoDB monitor also includes information of mutex waits and SHOW ENGINE INNODB MUTEX includes semaphores.

It is not only InnoDB that uses synchronization objects in MySQL; for example, the table open cache is also guarded by a mutex. However, in most cases, when you encounter problems with contention on the synchronization objects, it is related to InnoDB as that is where the pressure of high concurrency operations is usually the largest, and for InnoDB there are readily available monitoring tools for investigating the contention. For this reason, only the InnoDB will be discussed here.

The mutexes and semaphores are much more difficult to study than the data locks as it is not possible to pause the code execution while the locks are in place and study them directly. (Well it is, but that requires using a debugger such as gdb and the use of breakpoints.) Even with the synchronization waits enabled in the Performance Schema, you will usually fall short as there will be some many waits that even the long history table with a default of 10000 rows will quickly get exhausted even by a single connection as it can be seen from Listing 7-4.

Listing 7-4. Example of synchronization waits   
-- Connection Processlist ID Thread ID Event ID   
-- 1 638 1057 6   
-- 2 639 1058 6   
-- Connection 1   
Connection 1> UPDATE performance_schema_setup_instrument SET ENABLED $=$ 'YES', TIMED $=$ 'YES' WHERE NAME LIKE 'wait/synch/%';   
Query OK, 323 rows affected (0.0230 sec)   
Rows matched: 323 Changed: 323Warnings: 0

```txt
Connection 1> UPDATE performance_schema_setup Consumers
SET ENABLED = 'YES'
WHERE NAME IN ('events_waits_current', 'events_waits_history_long');
Query OK, 2 rows affected (0.0004 sec)
Rows matched: 2 Changed: 2Warnings: 0
-- Connection 2
Connection 2> UPDATE world.city
SET Population = Population + 1
WHERE CountryCode = 'USA';
Query OK, 274 rows affected (0.1522 sec)
Rows matched: 274 Changed: 274Warnings: 0
-- Connection 1
Connection 1> SELECT REPLACE(event_name, 'wait/synch/', '') AS event,
COUNT(*) FROM performance_schema.events_waits_history_long
WHERE thread_id = 1058
AND event_name LIKE 'wait/synch/%'
GROUP BY event_name
WITH ROLLUP
ORDER BY COUNT(*);
+-----------------------------------+
| event | COUNT(*) |
+-----------------------------------+
| mutex/sql/MYSQL_BIN_LOG::LOCK_done | 1 |
| mutex/innodb/purge_sys_pq_mutex | 1 |
| mutex/sql/MYSQL_BIN_LOG::LOCK_SYNC | 1 |
| mutex/sql/MYSQL_BIN_LOG::LOCK_log | 1 |
| mutex/mysql/xio_shutdown | 1 |
| mutex/sql/LOCKPlugin | 1 |
| mutex/sql/LOCKslave_trans_dep_tracking | 1 |
| mutex/sql/MYSQL_BIN_LOG::LOCK_binlog_end_pos | 1 |
| mutex/sql/MYSQL_BIN_LOG::LOCK_commit | 1 | 
```

```txt
| mutex/sql/MYSQL_BIN_LOG::LOCK_xids | 1 |
| --- | --- |
| sxlock/innodb/rsegs_lock | 1 |
| sxlock/innodb/undoSpaces_lock | 1 |
| mutex/sql/MYSQL_BIN_LOG::LOCK_SYNC_queue | 2 |
| mutex/innodb/lock_sys_table_mutex | 2 |
| mutex/sql/MYSQL_BIN_LOG::LOCK Flush_queue | 2 |
| mutex/sql/Gtid_state | 2 |
| mutex/sql/LOCK_table_cache | 2 |
| mutex/sql/MYSQL_BIN_LOG::LOCK_commit_queue | 2 |
| mutex/sql/THD::LOCK_thd_query | 2 |
| mutex/innodb/undo_space_rseg_mutex | 3 |
| mutex.sql/THD::LOCK_thd_data | 3 |
| rwlock/sql/gtid_commitROLLBACK | 3 |
| mutex/mysys/THR_LOCK_open | 4 |
| mutex/sql/THD::LOCK_query計劃 | 4 |
| mutex/innodb/flush_list_mutex | 5 |
| sxlock/innodb/index_tree_rw_lock | 5 |
| mutex/innodb/trx_undo_mutex | 274 |
| mutex/innodb/trx_sys_mutex | 279 |
| sxlock/innodb/bitical_search_latch | 288 |
| sxlock/innodb/bitical_search_latch | 550 |
| sxlock/innodb/log_sys_global_rw_lock | 551 |
| sxlock/innodb/log_sn_lock | 551 |
| mutex/innodb/log_sys_page_mutex | 554 |
| mutex/innodb/trx_mutex | 850 |
| NULL | 3950 | 
```

35 rows in set (0.0173 sec)

```sql
Connection 1> UPDATE performance_schema_setup_instruments  
SET ENABLED = 'NO',  
TIMED = 'NO'  
WHERE NAME LIKE 'wait/synch/%';  
Query OK, 323 rows affected (0.0096 sec) 
```

Rows matched: 323 Changed: 323 Warnings: 0

```sql
Connection 1> UPDATE performance_schema_setup Consumers  
SET ENABLED = 'NO'  
WHERE NAME IN ('events_waits_current', 'events_waits_history_long');  
Query OK, 2 rows affected (0.0004 sec) 
```

Rows matched: 2 Changed: 2 Warnings: 0

In this simple example, almost 4000 (the NULL row at the bottom of the result of querying events_waits_history_long) synchronization objects were requested. The exact list of waits and the number of them will vary from execution to execution and system to system depending on the state of the system (like whether the data is already in the buffer pool) and the configuration. If there is enough other activity, the number may also be much lower as some of the waits may have been pushed out of the events_ waits_history_long table by newer events. To complicate matters, background threads also generate wait events, so even if the system has no connections, wait events are created.

While it is hard to set up test cases that demonstrate the use of individual synchronization objects, the good news is that you as an end user mostly need to worry about contention, and the SHOW ENGINE INNODB STATUS and SHOW ENGINE INNODB MUTEX statements will provide you information about the contention of InnoDB mutexes and semaphores.

In general, you will need to study the source code to understand what the wait is for; however, alone considering the file can often provide a good indication of the functional area where the contention happens. Table 7-2 shows a few examples of how to map the file name provided by the mutex and semaphore information to the functional area. The source code path is relative to storage/innobase which contains the implementation of the InnoDB storage engine.

Table 7-2. Mutex and semaphore file names and their functional area   

<table><tr><td>File Name</td><td>Source Code Path</td><td>Functional Area</td></tr><tr><td>btr0sea.cc</td><td>btr/btr0sea.cc</td><td>The adaptive hash index.</td></tr><tr><td>buf0buf.cc</td><td>buf/buf0buf.cc</td><td>The buffer pool.</td></tr><tr><td>buf0flu.cc</td><td>buf/buf0flu.cc</td><td>The buffer pool flushing algorithm.</td></tr><tr><td>dict0dict.cc</td><td>dict/dict0dict.cc</td><td>The InnoDB data dictionary.</td></tr><tr><td>syncosharded_rw.h</td><td>include/syncosharded_rw.h</td><td>The sharded read-write lock for threads.</td></tr><tr><td>hashohash.cc</td><td>ha/Hashohash.cc</td><td>For protecting hash tables.</td></tr><tr><td>fil0fil.cc</td><td>fil/fil0fil.cc</td><td>The tablespace memory cache.</td></tr></table>

Other than for mutexes and semaphores implemented in header files, in general, you get to the source code file by using the name before the 0 in the file name (e.g., btr in btr0sea.cc) as the name of the directory and then the file name itself. If you open the file in an editor, then just after the license and copyright header, you will see a brief comment describing what the file is for, for example, from storage/innobase/btr/ btr0sea.cc:

/** @file btr/btr0sea.cc

The index tree adaptive search

Created 2/17/1996 Heikki Tuuri

*************************************************************************/

So, the btr0sea.cc file implements the adaptive search on the index tree of which the adaptive hash index is part of (and where the contention most commonly occurs).

# WHY INNOBASE? A BRIEF HISTORY OF INNODB

It may have puzzled you why the path to the InnoDB source code is storage/innobase/ using “innobase” rather than “innodb.” To understand that, you need to dive into the history of InnoDB – which turns out to be quite interesting.

Innobase was a company founded by Heikki Tuuri (yep, the same that is listed in the comment for the file storage/innobase/btr/btr0sea.cc) in 1995, the same year as the initial release of MySQL, but at that time the two had nothing to do with each other. Innobase was used to develop InnoDB which at the time meant to be an independent product. It was not until later when MySQL added support for third-party storage engines that Heikki released InnoDB as open source and it was integrated with MySQL.

In 2005, Oracle acquired Innobase and thus InnoDB which made for an interesting situation with MySQL’s main transactional storage engine (another much less used engine was Berkley DB, BDB, which also was acquired by Oracle) being maintained by a competitor. This was one reason for the effort to develop the Falcon storage engine for MySQL 6. However, before that work was completed, Sun Microsystems acquired MySQL, and Oracle in turn acquired Sun Microsystems, so in 2010, MySQL and InnoDB were finally part of the same company, and today InnoDB and MySQL are developed by the same unit within Oracle. This also meant that the Falcon storage engine was abandoned and never released with a general availability (GA) status.

While Innobase as a company has disappeared a long time ago, its name still lives on in the MySQL source code both in the path to the InnoDB source code and as names within the source code.

# Summary

In this chapter the InnoDB data-level locks as well as mutexes and rw-lock semaphores have been covered. These locks are all important to support concurrent access to the InnoDB data which is one of InnoDB’s strengths.

First, the record locks and next-key locks were discussed. These are usually what are meant when discussing InnoDB record locks. The next-key locks are the default locks in InnoDB and protect the record as well as the gab before the record. Second, the concept of gap locks was discussed. When mentioning gap locks, it refers to protecting the space between two records without protecting the record itself. Third, the related concepts of predicate and page locks that are used with spatial indexed were covered.

Fourth and fifth were two lock types that you will not encounter to the same degree as the first three lock types. The insert intention locks are as the name suggest used in connection with inserting data, and the auto-increment lock is used to ensure that autoincrement values are assigned correctly.

# Chapter 7 Inn oDB Locks

Sixth and last was a discussion of mutexes and rw-lock semaphores primarily in InnoDB. These are the most complex locks to work with and to a larger degree require studying the source code.

That concludes the overview of the locks available in MySQL and InnoDB. The next chapter moves on to the discussion of what happens when a lock cannot be obtained.

# Working with Lock Conflicts

The whole idea of locks is to restrict access to objects or records to avoid conflicting operations to concurrently access the object or records in a safe way. That means that, sometimes, a lock cannot be granted. What happens in that case? It depends on the locks requested and the circumstances. Metadata (including explicitly requested table locks) and InnoDB locks operate with a timeout, and for some lock cases explicit deadlock detection exist.

It is important to understand that failures to obtain locks are a fact of life when working with databases. In principle you can use very coarse-grained locks and avoid failed locks except for timeouts – this is what the MyISAM storage engine does with very poor write concurrency as a result. However, in practice, to allow for high concurrency of write workloads, fine-grained locks are preferred which also introduce the possibility of deadlocks.

The conclusion is that you should always make your application prepared to retry getting a lock or fail gracefully. This applies whether it is an explicit or implicit lock.

Tip Always be prepared to handle failures to obtain locks. Failing to get a lock is not a catastrophic error and should not normally be considered a bug. That said, as discussed in Chapter 9, “Reducing Locking Issues,” there are techniques to reduce lock contention that are worth having in mind when developing an application.

The rest of this chapter will discuss how InnoDB chooses which transaction should first be granted a lock request when there are multiple requests for the same data lock, the compatibility of InnoDB data locks, as well as the specifics of table-level timeouts, record-level timeouts, InnoDB deadlocks, and InnoDB mutex and semaphore waits.

# Contention-Aware Transaction Scheduling (CATS)

An important decision when there are multiple requests for the same lock is to decide in which order locks should be granted. The simplest solution, and the most commonly used in databases, is to maintain a queue and serve the requests on a first-in-first-out (FIFO) principle. This is also how locks were granted in MySQL 5.7 and earlier; however in MySQL 8, a new scheduling algorithm was implemented.

The new implementation is based on the contention-aware transaction scheduling (CATS) algorithm developed by Professor Barzan Mozafari’s team at the University of Michigan and implemented in MySQL by Sunny Bains in collaboration with Professor Mozafari’s team, particularly Jiamin Huang.

Tip If you want to learn more about the CATS algorithm, then https:// mysqlserverteam.com/contention-aware-transaction-schedulingarriving-in-innodb-to-boost-performance/ is a good starting point, and there are links to two of the research papers in the comments – the main paper being http://web.eecs.umich.edu/~mozafari/php/data/uploads/ pvldb_2018_sched.pdf. Another source is https://dev.mysql.com/ doc/refman/en/innodb-transaction-scheduling.html in the reference manual.

The CATS algorithm works on the principle that transactions that already hold a large number of locks are most important to drive to completion, so their locks can be released as quickly as possible to the benefit of all transactions. One potential downside of this approach is that if there continuously are transactions with many existing locks waiting for a given lock, then they can potentially starve transactions with few locks from ever getting the lock. To prevent that, the algorithm has safeguards to prevent starvation. The safeguard works by adding a barrier at the end of the current queue of lock requests, and all requests that are ahead of the barrier are handled before later arriving requests will be considered.

The primary benefit of the CATS algorithm is under high concurrency workloads, and until MySQL 8.0.20, it was only used when InnoDB detected heavy lock contention. The algorithm was improved in 8.0.20 to improve the scalability, and the trx_schedule_ weight column was added to information_schema.INNODB_TRX, so it is possible to

query the current weight a transaction in the LOCK WAIT state has according to the CATS algorithm. At the same time, it was changed so the CATS algorithm is always used, and the FIFO algorithm has been retired.

# InnoDB Data Lock Compatibility

Remember when discussing lock access level compatibility, the rules were relatively simple. However, whether two InnoDB data locks are compatible with each other is very complex to determine. It becomes particularly interesting as the relationship is not symmetric, that is, a lock may be allowed in the presence of another lock, but not vice versa. For example, an insert intention lock must wait for a gap lock, but a gap lock does not have to wait for an insert intention lock. Another example (of lack of transitivity) is that a gap plus record lock must wait for a record-only lock, and an insert intention lock must wait for a gap plus record lock, but an insert intention lock does not need to wait for a record-only lock.

What does that mean to you? It means that when you investigate lock contention issues, you need to be aware that the lock order is significant, so when reproducing the issue, all locks must be obtained in the same order.

Enough about the theory behind InnoDB algorithm for handling lock contention and what may cause a lock not to be granted. What happens when a lock cannot be granted is the next topic to be discussed.

# Metadata and Backup Lock Wait Timeouts

When you request a flush, metadata, or backup lock, the attempt to get the lock will time out after lock_wait_timeout seconds. The default timeout is 31,536,000 seconds (365 days). You can set the lock_wait_timeout option dynamically and both at the global and session scopes, which allows you to adjust the timeout to the specific needs for a given process.

When a timeout occurs, the statement fails with an ER_LOCK_WAIT_TIMEOUT (error number 1205) error as shown in Listing 8-1.

Listing 8-1. Lock wait timeout for table lock request   
```txt
-- Connection Processlist ID Thread ID Event ID
-- 1 647 1075 6
-- 2 648 1076 6 
```

```sql
-- Connection 1
Connection 1> LOCK TABLES world.city WRITE;
Query OK, 0 rows affected (0.0015 sec) 
```

```sql
-- Connection 2  
Connection 2> SET SESSION lock_wait_timeout = 5;  
Query OK, 0 rows affected (0.0003 sec) 
```

ERROR: 1205: Lock wait timeout exceeded; try restarting transaction   
```txt
Connection 2> LOCK TABLES world.city WRITE; 
```

```txt
-- Connection 1  
Connection 1> UNLOCK TABLES;  
Query OK, 0 rows affected (0.0003 sec) 
```

The session value of lock_wait_timeout is set to 5 seconds to reduce how long a time Connection 2 will block for when it attempts to get the write lock on the world.city table. After waiting for 5 seconds, the error is returned with the error number set to 1205.

The recommended setting for the lock_wait_timeout option depends on the requirements of the application. It can be an advantage to use a small value to prevent the lock request to block other queries for a long time. This will typically require you to implement handling of a lock request failure, for example, by retrying the statement. A large value can on the other hand be useful to avoid having to retry the statement.

For the FLUSH TABLES statement, also remember that it interacts with the lowerlevel table definition cache (TDC) version lock which may mean that abandoning the statement does not allow subsequent queries to progress. In that case, it can be better to have a high value for lock_wait_timeout to make it clearer what the lock relationship is.

# InnoDB Lock Wait Timeouts

When a query requests a record-level lock in InnoDB, it is subject to a timeout similarly to the timeout for flush, metadata, and backup locks. Since record-level lock contention is more common than table-level lock contention, and record-level locks increase the potential for deadlocks, the timeout defaults to 50 seconds. It can be set using the innodb_lock_wait_timeout option which can be set both for the global and session scopes.

When a timeout occurs, the query fails with the ER_LOCK_WAIT_TIMEOUT error (error number 1205) just like for a table-level lock timeout. Listing 8-2 shows an example where an InnoDB lock wait timeout occurs.

Listing 8-2. Example of an InnoDB lock wait timeout   
ERROR: 1205: Lock wait timeout exceeded; try restarting transaction   
-- Connection Processlist ID Thread ID Event ID   
-- 1 656 1087 6   
-- 2 657 1088 6   
-- Connection 1   
Connection 1> START TRANSACTION;   
Query OK, 0 rows affected (0.0002 sec)   
Connection 1> UPDATE world.city SET Population $=$ Population + 1 WHERE $\mathrm{ID} = 130$ Query OK, 1 row affected (0.0006 sec)   
Rows matched: 1 Changed: 1Warnings: 0   
-- Connection 2   
Connection 2> SET SESSION innodb_lock_wait_timeout $=$ 3 Query OK, 0 rows affected (0.2621 sec)   
Connection 2> UPDATE world.city SET Population $=$ Population + 1 WHERE $\mathrm{ID} = 130$

-- Connection 1 Connection 1> ROLLBACK; Query OK, 0 rows affected (0.0751 sec)

In this example, the lock wait timeout for Connection 2 is set to 3 seconds, so it is not necessary to wait the usual 50 seconds for the timeout to occur.

When the timeout occurs, the innodb_rollback_on_timeout option defines how much of the work done by the transaction is rolled back. When innodb_rollback_on_ timeout is disabled (the default), only the statement that triggered the timeout is rolled back. When the option is enabled, the whole transaction is rolled back. The innodb rollback_on_timeout option can only be configured at the global level, and it requires a restart to change the value.

Caution It is very important that a lock wait timeout is handled as otherwise it may leave the transaction with locks that are not released. If that happens, other transactions may not be able to acquire the locks they require. So, you need ensure that you either retry the remaining part of the transaction, explicitly roll back the transaction, or enable innodb_rollback_on_timeout to automatically roll back the transaction on a lock wait timeout.

It is in general recommended to keep the timeout for InnoDB record-level locks low. Often it is best to lower the value from the default 50 seconds. The longer a query is allowed to wait for a lock, the larger the potential for other lock requests to be affected which can lead to other queries stalling as well. It also makes deadlocks more likely to occur. If you disable deadlock detection (discussed next), you should use a very small value for innodb_lock_wait_timeout such as 1 or 2 seconds as you will be using the timeout to detect deadlocks. Without deadlock detection, it is also recommended to enable the innodb_rollback_on_timeout option.

# Deadlocks

Deadlocks sound like a very scary concept, but you should not let the name deter you. Just like lock wait timeout, deadlocks are a fact of life in the world of high-concurrency databases. What it really means is that there is a circular relationship between the lock requests as illustrated by a traffic gridlock in Figure 8-1. The only way to resolve

the gridlock is to force one of the requests to abandon. In that sense, a deadlock is no different from a lock wait timeout. In fact, you can disable deadlock detection in which case, one of the locks will end up with a lock wait timeout instead.

![](images/c3a4ccfbe637b792d68a63a3dd5e25e559c6a0391eeaaa6aef58dfb8d9d6460b.jpg)  
Figure 8-1. A traffic gridlock

So why are there deadlocks at all if they are not really needed? Since deadlocks occur when there is a circular relationship between the lock requests, it is possible for InnoDB to detect them as soon as the circle is completed. This allows InnoDB to tell the user immediately that a deadlock has occurred without having to wait for the lock wait timeout to happen. It is also useful to be told that a deadlock has occurred as it often provides opportunities to improve data access in the application. You should thus consider deadlocks a friend rather than a foe. Figure 8-2 shows an example of two transactions querying a table which causes a deadlock.

# Transaction 1

# city Table

# Transaction 2

![](images/4db849c93e013c9c45ad8528057064f8a100e31b905498024e3d4766f8f20abb.jpg)  
Figure 8-2. Example of two transactions causing a deadlock

In the example, transaction 1 first updates the row with $\mathrm { I D } ~ = ~ 1 3 0$ and then the row with $\mathrm { I D } \ = \ 3 8 0 5 .$ In between, transaction 2 updates first the row with $\mathrm { I D } ~ = ~ 3 8 0 5$ and then the row with $\mathrm { I D } ~ = ~ 1 3 0$ . This means that by the time transaction 1 tries to update $\mathrm { ~ I D ~ } =$ 3805, transaction 2 already has a lock on the row. Transaction 2 can also not proceed as it cannot get a lock on $\mathrm { I D } ~ = ~ 1 3 0$ because transaction 1 already holds that. This is a classic example of a simple deadlock. The circular lock relationship is also shown in Figure 8-3.

# Transaction 1

![](images/96193320a46cee4e62debf7e0614a0fb37800f26d1d2b73cfdcfc9969e8ed090.jpg)  
Figure 8-3. The circular relationship of the locks causing the deadlock

# Transaction 2

In this figure, it is clear which lock is held by transactions 1 and 2 and which locks are requested and how the conflict can never be resolved without intervention. That makes it qualify as a deadlock.

In the real world, deadlocks are often more complicated. In the example that has been discussed here, only primary key record locks have been involved. In general, often secondary keys, gap locks, and possible other lock types are also involved. There may also be more than two transactions involved. The principle, however, remains the same.

Note A deadlock may even occur with as little as one query for each of two transactions. If one query reads the records in ascending order and the other on descending order, it is possible to get a deadlock.

When a deadlock occurs, InnoDB chooses the transaction that has “done the least work” to become a victim. This is similar to the “Shoot The Other Node In The Head” (STONITH) approach used in some high availability solutions such as MySQL NDB Cluster except here it is a transaction that is being “shot in the head.” You can check the

trx_weight column in the information_schema.INNODB_TRX view to see the weight used by InnoDB (the more work done, the higher weight). In practice this means that the transaction that holds the fewest locks will be rolled back. When this occurs, the query in the transaction that is chosen as the victim fails with the error ER_LOCK_DEADLOCK returned (error code 1213), and the transaction is rolled back to release as many locks as possible. An example of a deadlock occurring is shown in Listing 8-3.

Listing 8-3. Example of a deadlock   
-- Connection Processlist ID Thread ID Event ID   
1 659 1093 6   
2 660 1094 6   
-- Connection 1   
Connection 1> START TRANSACTION;   
Query OK, 0 rows affected (0.0002 sec)   
Connection 1> UPDATE world.city SET Population $=$ Population + 1 WHERE $\mathrm{ID} = 130$ Query OK, 1 row affected (0.0098 sec)   
Rows matched: 1 Changed: 1Warnings: 0   
-- Connection 2   
Connection 2> START TRANSACTION;   
Query OK, 0 rows affected (0.0003 sec)   
Connection 2> UPDATE world.city SET Population $=$ Population + 1 WHERE $\mathrm{ID} = 3805$ Query OK, 1 row affected (0.0009 sec)   
Rows matched: 1 Changed: 1Warnings: 0   
Connection 2> UPDATE world.city SET Population $=$ Population + 1 WHERE $\mathrm{ID} = 130$

ERROR: 1213: Deadlock found when trying to get lock; try restarting transaction   
```sql
-- Connection 1  
Connection 1> UPDATE world.city  
SET Population = Population + 1  
WHERE ID = 3805; 
```

```txt
-- Connection 2  
Query OK, 1 row affected (0.1019 sec)  
Rows matched: 1 Changed: 1Warnings:  
-- Connection 1  
Connection 1> ROLLBACK;  
Query OK, 0 rows affected (0.0002 sec)  
-- Connection 2  
Connection 2> ROLLBACK;  
Query OK, 0 rows affected (0.0293 sec) 
```

A deadlock can be even simpler than in this example (though it is rare unless you use locking SELECT statements either explicitly or using the SERIALIZABLE transaction isolation level). Listing 8-4 shows a deadlock that occurs using just a single row.

Listing 8-4. A single row deadlock   
```txt
-- Connection Processlist ID Thread ID Event ID   
-- 1 663 1097 6   
-- 2 664 1098 6   
-- Connection 1   
Connection 1> START TRANSACTION;   
Query OK, 0 rows affected (0.0004 sec) 
```

Chapt er 8 Wor king wit h Lock Conflicts   
```sql
Connection 1> SELECT * FROM world.city WHERE ID = 130 FOR SHARE; 
```

```txt
+ 
```

```txt
| ID | Name | CountryCode | District | Population | 
```

```txt
+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
```

```txt
| 130 | Sydney | AUS | New South Wales | 3276207 | 
```

```txt
+---++---++---++---++---++---++---++---
```

```txt
1 row in set (0.0005 sec) 
```

```sql
-- Connection 2 
```

```txt
Connection 2> START TRANSACTION; 
```

```txt
Query OK, 0 rows affected (0.0002 sec) 
```

```txt
Connection 2> UPDATE world.city 
```

```txt
SET Population = Population + 1 
```

WHERE $\text{ID} = 130$

```sql
-- Connection 1 
```

```txt
Connection 1> UPDATE world.city 
```

```txt
SET Population = Population + 1 
```

WHERE $\text{ID} = 130$

```txt
Query OK, 1 row affected (0.0447 sec) 
```

-- Connection 2   
```txt
Rows matched: 1 Changed: 1Warnings: 0 
```

ERROR: 1213: Deadlock found when trying to get lock; try restarting transaction   
```sql
-- Connection 1 
```

```txt
Connection 1> ROLLBACK; 
```

```txt
Query OK, 0 rows affected (0.0280 sec) 
```

```sql
-- Connection 2 
```

```txt
Connection 2> ROLLBACK; 
```

```txt
Query OK, 0 rows affected (0.0003 sec) 
```

In this case, Connection 2 becomes a victim of a deadlock without having ever being granted a record lock. This deadlock happens because InnoDB currently does not allow the request to upgrade the shared lock to an exclusive lock in Connection 1

to jump ahead of Connection 2’s lock request. It is something that can potentially be implemented, but because these scenarios are relatively rare, it has not been done yet. That said, if you have foreign keys, a DML statement may take a shared lock on the other table in the foreign key relationship (see also Chapter 10), so if a subsequent statement in the same transaction tried to upgrade that shared lock, then you can see the same kind of deadlocks as in this example.

In most cases, the automatic deadlock detection is great to avoid queries stalling for longer than necessary. Deadlock detection is not for free though. For MySQL instances with a very high query concurrency, the cost of looking for deadlocks can become significant, and you are better off disabling the deadlock detection which is done by setting the innodb_deadlock_detect option to OFF. That said, in MySQL 8.0.18 and later, the deadlock detection has been moved to a dedicated background thread which improves the performance.

If you do disable deadlock detection, it is recommended to set innodb_lock_ wait_timeout to a very low value such as 1 second to quickly detect lock contention. Additionally, enable the innodb_rollback_on_timeout option to ensure the locks are released.

The last kind of lock conflict handling occurs with InnoDB mutexes and semaphores.

# InnoDB Mutex and Semaphore Waits

When InnoDB requests a mutex or rw-lock semaphore and it cannot be immediately obtained, it will have to wait. Because the waits happen at a lower level than data locks, InnoDB will resort to one of two approaches while waiting. It can enter a loop and poll for the lock to become available, or it can suspend the thread and make it available for other tasks.

Polling allows the lock to be obtained more quickly but it keeps the CPU thread busy, and polling can cause CPU cache invalidation for other threads. There are three configuration options that can be used to control the behavior:

• innodb_spin_wait_delay: When polling, InnoDB will calculate a random number between zero and innodb_spin_wait_delay. This is multiplied with innodb_spin_wait_pause_multiplier to determine the number of PAUSE instructions that occur in the poll loop. The random number of PAUSE events is used to reduce the impact of cache invalidation. Smaller values – or even 0 – can potentially help

on systems with a single shared fast CPU cache. Larger values can reduce the impact of cache invalidation particularly on multi-CPU systems.

• innodb_spin_wait_pause_multiplier: The multiplier used with the spin wait delay. This option is new from MySQL 8.0.16 and was introduced to accommodate for the change in the duration of the PAUSE instructions introduced with the Skylake generation of processors. The primary use of changing the value is using MySQL on architectures that have a different duration of the PAUSE instructions than $\mathrm { x 8 6 / x 8 6 - 6 4 }$ before the Skylake generation. In earlier releases, the multiplier is hardcoded to 50 which is also the default value for innodb_spin_wait_pause_multiplier.   
• innodb_sync_spin_loops: The number of spin loops to perform before suspending the thread. The lower the value, the quicker the CPU thread is made available for other tasks at the expense of more context switches. When the spin loops are exceeded, the OS waits counter for the rw-lock increments. The higher the value, the quicker the lock can be obtained at the cost of higher CPU usage. The default is 30.

You will rarely have to adjust these settings; however, in rare cases, they can improve performance to some degree. That said, if you are not using the latest MySQL version, you may benefit in terms of reducing mutex/semaphore contention more from upgrading than tuning these options. In all cases, if you decide to change these settings, make sure you test the performance thoroughly on the same architecture and hardware configuration as your production system and with a workload that is a very good representation of your production workload.

If an InnoDB mutex or rw-lock semaphore wait cannot be obtained immediately, InnoDB will also register this internally. The relevant counter exposed through SHOW ENGINE INNODB MUTEX will increment (though only mutexes and rw-locks with at least one OS wait are displayed), and if you generate the InnoDB monitor report while the wait is ongoing, it will be included in the SEMAPHORES wait section of the rapport. If a wait continues without progress being detected for more than 240 seconds, InnoDB will automatically enable the InnoDB monitor and write the output to the error log, so you can investigate the issue. If no progress is detected for another 600 seconds, then InnoDB will shut down MySQL as a preventive measure as it assumes an unresolvable situation

has occurred. In that case, you will see an error explaining the reason for the shutdown, for example (yes, the duration printed is somewhat misleading as it is the time since the “long semaphore wait” condition triggered at 240 seconds)

2020-07-05T09:30:24.151782Z 0 [ERROR] [MY-012872] [InnoDB] Semaphore wait has lasted > 600 seconds. We intentionally crash the server because it appears to be hung.

By crashing the server, InnoDB ensures that if a bug internally in InnoDB has been encountered, the situation is resolved, but at the cost that MySQL will have to restart and go through a crash recovery. For this reason, it is considered a last resort, and at present, the timeout is not configurable.

# Note When executing CHECK TABLE, the timeout threshold is increased to 7200 seconds (2 hours).

A shutdown like this typically happens for one of two reasons:

There is a hardware or operating system issue that prevents InnoDB progressing.   
There is a bug in InnoDB, for example, that progress for a slow operation is not detected or a deadlock has occurred for the acquisition of a mutex or semaphore.

If you encounter a shutdown like this, verify from the InnoDB monitor outputs in the error log where the wait occurred. In some cases, you can use the thread ids to determine which query is causing the wait. You should also check your system logs to verify the health of your hardware and whether there are any indications of problems at the operating system level.

# Summary

This chapter has provided an overview of what happens when a lock cannot immediately be obtained. First, the contention-aware transaction scheduling (CATS) algorithm was described. That is used in MySQL 8 to allow transactions that already hold many locks to have their lock requests granted quicker, so their locks also can be more quickly released again.

Second, it was discussed how the compatibility of InnoDB data locks is a very a complex issue which means that the lock order must be taken into account when trying to reproduce issues.

The rest of the chapter went through metadata, backup, and InnoDB lock wait timeouts, deadlocks, and InnoDB mutex and rw-lock semaphore waits. Lock waits and deadlocks are naturally occurring in high concurrency systems and should not on their own be a cause for alarm. The main issue is when they become frequent. The default lock wait timeouts may also be too long, so reducing them and handling the timeout can be an option.

Now that you have an understanding of how locks work and how lock requests can fail, you need to consider how you can reduce the impact of locking which is the topic of the next chapter.

# Reducing Locking Issues

Remember that the locking in MySQL and InnoDB is a means to provide concurrent access, and in general the fine-grained locking of InnoDB allows for a highly concurrent workload. Yet, if you have excessive locking, it will cause reduced concurrency and query pileups, and in the worst case, it can cause an application to come to a grinding halt and cause a poor user experience.

Thus, it is important to have locks in mind when you write an application and design the schema for its data and access. The strategies to reduce locking include adding indexes, changing the transaction isolation level, changing the configuration, and preemptive locking. This chapter covers each of these strategies.

Tip Do not be carried away in optimizing locks. If you only occasionally encounter lock wait timeouts and deadlocks, it is usually better to retry the query or transaction rather than spend time avoiding the issue. How frequent is too frequent depends on your workload, but several retries every hour will not be an issue for many applications.

# Transaction Size and Age

An important strategy to reduce lock issues is to keep your transactions small and to avoid delays that keep the transactions open for longer than necessary. Among the most common causes of lock issues are transactions that modify a large number of rows or that are active for longer than necessary.

The size of the transaction is the amount of work the transaction does, particularly the number of locks it takes, but the time the transaction takes to execute is also important. As some of the other topics in this discussion will address, you can partly reduce the impact through indexes and the transaction isolation level. However, it is

also important to have the overall result in mind. If you need to modify many rows, ask yourself if you can split the work into smaller batches or whether it is required that everything is done in the same transaction. It may also be possible to split out some preparation work and do it outside the main transaction.

The duration of the transaction is also important. One common problem is connections using autocommit $\ = \ 0 .$ . This starts a new transaction every time a query (including SELECT) is executed without an active transaction, and the transaction is not completed until an explicit COMMIT or ROLLBACK is executed, a DDL statement is executed, or the connection is closed. Some connectors disable autocommit by default, so you may be using this mode without realizing it which can leave transactions open for hours by mistake.

Tip Enable the autocommit option unless you have a specific reason to disable it. When you have autocommitting enabled, InnoDB can also for many SELECT queries detect it is a read-only transaction and reduce the overhead of the query.

Another pitfall is to start a transaction and perform slow operations in the application while the transaction is active. This can be data that is sent back to the user, interactive prompts, or file I/O. Make sure that you do these kinds of slow operations when you do not have an active transaction open in MySQL.

# Indexes

Indexes reduce the amount of work performed to access a given row. That way indexes are a great tool to reduce locking as only records accessed while executing the query will be locked.

Consider a simple example where you query cities with the name Sydney in the world.city table:

START TRANSACTION;

SELECT *

FROM world.city

WHERE Name $=$ 'Sydney'

FOR SHARE;

The FOR SHARE option is used to force the query to take a shared lock on the records read. By default, there is no index on the Name column, so the query will perform a full table scan to find the rows needed in the result. Without an index, there are 4103 record locks (24 of the locks are on the supremum pseudo-record of the primary key) as shown in Listing 9-1.

Listing 9-1. Record locks without an index on the Name column   
```txt
-- Connection Processlist ID Thread ID Event ID
-- 1 697 1143 6
-- 2 698 1144 6 
```

```sql
-- Connection 1  
Connection 1> START TRANSACTION;  
Query OK, 0 rows affected (0.0002 sec) 
```

```sql
Connection 1> SELECT ID, Name, CountryCode, District FROM world.city WHERE Name = 'Sydney' FOR SHARE; 
```

```txt
+  
| ID | Name | CountryCode | District |  
+  
| 130 | Sydney | AUS | New South Wales |  
+  
1 row in set (0.0034 sec) 
```

```sql
-- Connection 2   
Connection 2> SELECT index_name, lock_type, lock_mode, COUNT(*) FROM performance_schema.data_locks WHERE object_schema = 'world' AND object_name = 'city' AND thread_id = 1143   
GROUP BY index_name, lock_type, lock_mode; 
```

```txt
+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+--+--  
| index_name | lock_type | lock_mode | COUNT(*) |  
+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+--- +  
| NULL | TABLE | IS | 1 |  
| PRIMARY | RECORD | S | 4103 |
```

2 rows in set (0.0323 sec)

```sql
-- Connection 1  
Connection 1> ROLLBACK;  
Query OK, 0 rows affected (0.0005 sec) 
```

If you add an index on the Name column, the lock count decreases to a total of three record locks as shown in Listing 9-2.

Listing 9-2. Record locks with an index on the Name column   
```txt
-- Connection Processlist ID Thread ID Event ID
-- 1 699 1145 6
-- 2 700 1146 6 
```

```sql
-- Connection 1  
Connection 1> ALTER TABLE world.city  
ADD INDEX (Name);  
Query OK, 0 rows affected (1.5063 sec) 
```

```yaml
Records: O Duplicates: OWarnings: O 
```

```txt
Connection 1> START TRANSACTION;  
Query OK, 0 rows affected (0.0003 sec) 
```

```sql
Connection 1> SELECT ID, Name, CountryCode, District FROM world.city WHERE Name = 'Sydney' FOR SHARE; 
```

```sql
+  
| ID | Name | CountryCode | District  
+  
| 130 | Sydney | AUS | New South Wales  
+  
1 row in set (0.0004 sec)  
-- Connection 2  
Connection 2> SELECT index_name, lock_type, lock_mode, COUNT(*) FROM performance_schema.data_locks WHERE object_schema = 'world' AND object_name = 'city' AND thread_id = 1145 GROUP BY index_name, lock_type, lock_mode;  
+  
| index_name | lock_type | lock_mode | COUNT(*)  
+  
| NULL | TABLE | IS | 1  
| Name | RECORD | S | 1  
| PRIMARY | RECORD | S,REC_NOT_GAP | 1  
| Name | RECORD | S,GAP | 1  
+  
4 rows in set (0.0011 sec)  
-- Connection 1  
Connection 1> ROLLBACK; Query OK, 0 rows affected (0.0004 sec)  
Connection 1> ALTER TABLE world.city DROP INDEX Name;  
Query OK, 0 rows affected (0.3288 sec)  
Records: 0 Duplicates: 0Warnings: 0 
```

On the flip side, more indexes provide more ways to access the same rows which potentially can increase the number of deadlocks.

# Record Access order

Ensure that you to as large degree as possible access the records in the same order for different transactions. In the deadlock example discussed in Chapter 8, what led to the deadlock was that the two transactions accessed the rows in opposite order. If they had accessed the rows in the same order, 