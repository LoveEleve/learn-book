Defect

Detect

# Linux API

for Software Diagnostics

# Accelerated

With Category Theory in View

![](images/91bb28e420a707a83f8ac0b199666ed86b27e118907e11785ff16bd8f37979d9.jpg)

Dmitry Vostokov Software Diagnostics Services

Published by OpenTask, Republic of Ireland

Copyright $\circledcirc$ 2023 by OpenTask

Copyright $\circledcirc$ 2023 by Software Diagnostics Services

Copyright $\circledcirc$ 2023 by Dmitry Vostokov

All rights reserved. No part of this book may be reproduced, stored in a retrieval system, or transmitted, in any form or by any means, without the publisher’s prior written permission.

Product and company names mentioned in this book may be trademarks of their owners.

OpenTask books and magazines are available through booksellers and distributors worldwide.

For further information or comments, send requests to press@opentask.com.

A CIP catalog record for this book is available from the British Library.

ISBN-l3: 978-1-912636-62-4 (Paperback)

Revision 1.01 (June 2023)

# Contents

About the Author.. 5

Introduction...

Exercise L0 (GDB).... . 21

Exercise L0 (WinDbg)..... .. 24

x64 and A64.. .. 43

General Linux API Aspects . .. 61

Exercise L1 (GDB).. . 77

Exercise L1 (WinDbg) ........ ..... 85

Exercise L2 (GDB)........... ........ 100

Exercise L2 (WinDbg)..... ..... 103

Exercise L3 (GDB)....... ...... 109

Exercise L3 (WinDbg)..... .. 121

Exercise L4 (GDB)....... ..... 128

Exercise L5 (GDB)....... ...... 134

Exercise L6 (GDB)..... ..... 144

Exercise L6 (WinDbg) ................. ....... 147

Exercise L7 (GDB)..... . 163

Exercise L7 (WinDbg).. . 165

Linux API Formalization .. . 171

Linux API and Languages .............. ..... 187

Exercise L8 .........................

Exercise L9 ..... . 196

Linux API Classes.. .. 201

References and Resources.. . 225

# About the Author

![](images/b98a066c2b192396c7c9643dc018a08568d20900058fab8beb25546722bad280.jpg)

Dmitry Vostokov is an internationally recognized expert, speaker, educator, scientist, inventor, and author. He is the founder of the pattern-oriented software diagnostics, forensics, and prognostics discipline (Systematic Software Diagnostics), and Software Diagnostics Institute $\mathrm { \ D A + T A }$ : DumpAnalysis.org $^ +$ TraceAnalysis.org). Vostokov has also authored more than 50 books on software diagnostics, anomaly detection and analysis, software and memory forensics, root cause analysis and problem solving, memory dump analysis, debugging, software trace and log analysis, reverse engineering, and malware

analysis. He has over 25 years of experience in software architecture, design, development, and maintenance in various industries, including leadership, technical, and people management roles. Dmitry also founded Syndromatix, Anolog.io, BriteTrace, DiaThings, Logtellect, OpenTask Iterative and Incremental Publishing (OpenTask.com), Software Diagnostics Technology and Services (former Memory Dump Analysis Services) PatternDiagnostics.com, and Software Prognostics. In his spare time, he presents various topics on Debugging.TV and explores Software Narratology, its further development as Narratology of Things and Diagnostics of Things (DoT), Software Pathology, and Quantum Software Diagnostics. His current interest areas are theoretical software diagnostics and its mathematical and computer science foundations, application of formal logic, artificial intelligence, machine learning and data mining to diagnostics and anomaly detection, software diagnostics engineering and diagnostics-driven development, diagnostics workflow and interaction. Recent interest areas also include cloud native computing, security, automation, functional programming, applications of category theory to software diagnostics, development and big data, and diagnostics of artificial intelligence.

# Introduction

# Linux API

# for Software Diagnostics

# Accelerated

With Category Theory in View

![](images/037095addd1fbaee56c76674372676dc2779afd35cc5d352847cc82896a237c5.jpg)

Dmitry Vostokov Software Diagnostics Services

Hello everyone, my name is Dmitry Vostokov, and I teach this training course.

# Prerequisites

Development experience   
and (optional)   
Basic process core dump analysis

2023Software Diagnostics Services

To get most of this training, you are expected to have basic development experience and optional basic process core dump analysis experience. I assume you know what types, functions, and their parameters are. If you don’t have a core dump analysis experience, then you also learn some basics too because we use GDB and optionally the Microsoft debugger, WinDbg (classic) from Debugging Tools for Windows, or the WinDbg app (former WinDbg Preview) for some exercises. I explain some debugging and related concepts when necessary during the course.

# Training Goals

Review fundamentals of Linux APl   
Learn diagnostic analysis techniques   
See how Linux APl knowledge is used during diagnostics and debugging

2023 Software Diagnostics Services

Our primary goal is to learn Linux API in an accelerated fashion. So, first, we review Linux API fundamentals necessary for software diagnostics. Then we learn various analysis techniques for Linux API exploration. And finally, we see examples of how the knowledge of Linux API helps in diagnostics and debugging.

# Training Principles

Talk only about what I can show   
Lots of pictures   
Lots of examples   
Original content and examples

2023 Software Diagnostics Services

There were many training formats to consider, and I decided that the best way is to concentrate on slides and hands-on demonstrations you can repeat yourself as homework. Some of them are available as step-by-step exercises.

# Schedule

Review of relevant x64 and A64 disassembly   
General Linux APl aspects   
Linux APl formalization   
Linux APl and languages   
Linux APl classes   
Practical exercises

2023Software Diagnostics Services

The rough coverage or schedule includes general API aspects that can also be applicable to other operating systems. We also take a radical detour and introduce category theory in the API context. Our coverage is not only theoretical. We also do a tour through different API subsets and classes. An integral part of this training is practical exercises.

# Training Idea

Previous Accelerated Windows API training

Cybersecurity   
Memory dump analysis   
Reading Windows-based Code training

Experience writing Linux APl monitoring tools

2023Software Diagnostics Services

This training idea came from the previous Windows API training for security professionals who mentioned the need for Windows API knowledge and attendees of my memory dump analysis training courses who asked questions related to Windows API. I realized that since I have the Linux core dump analysis course, attendees of it would also benefit from similar training for Linux API too. This training may also fill some gaps from other training courses, such as Linux disassembly and reversing. Additional push came from my experience designing and implementing Linux API monitoring tools from the ground up.

# General Linux APl Aspects

O Header view   
Naming convention   
Basic type system   
Call types   
C Export/import functions   
PLT and GOT   
Virtual process address space   
O Calling convention   
C APl sequences   
API layers   
APl and system calls   
API source code   
Shared Libraries   
APl usage   
O APl internals

Static linking   
Delayed dynamic linking   
APl name patterns   
APl namespaces   
C APl syntagms/paradigms   
Marked API   
ADDR patterns   
DebugWare patterns   
C Memory analysis patterns   
API tracing   
C Trace and log analysis patterns   
O APl and errors   
O APl and functional programming   
0 APl and security   
APl and versioning

2023Software Diagnostics Services

The general Linux API aspects we plan to discuss are listed on this slide.

# Linux APl Formalization

APl compositionality   
Category theory language   
A view of category theory   
Category theory square   
APl category   
API functor   
APl diagram   
APl natural transformation   
Cross-platform API   
APl adjunction   
Informal n-APl   
APl and trace categories   
API I/O

2023Software Diagnostics Services

This training also includes an API formalization via Category Theory. We start with a brief overview of categories, functors, and other aspects using Linux API examples.

# Linux APl and Languages

○ C#   
Scala Native   
Golang   
Rust   
Python

2023Software Diagnostics Services

We also cover the basics of how Linux API is used in languages other than $\mathrm { C } / \mathrm { C } { + } { + }$ with template examples.

# Linux APl Classes

System configuration   
O File I/O   
File control   
Filesystem   
Dynamic memory   
Virtual memory   
Shared libraries   
Process   
IPC

C Job   
Signals   
Thread   
Networking   
Time   
Timers   
Tracing and logging   
Accounts   
Terminal

2023Software Diagnostics Services

A part of this training includes a tour of different Linux API subsets and classes.

# Links

○ Core Dumps

Included in Exercise L0

Exercise Transcripts

Included in this book

2023Software Diagnostics Services

# Exercise LO

Goal: Install GDB and check if GDB loads a core dump correctly   
Goal: Install WinDbg or Debugging Tools for Windows, or pull Docker image, and check that symbols are set up correctly   
0 Memory Analysis Patterns: Stack Trace; Incorrect Stack Trace   
\LAPI-Dumps\Exercise-L0-GDB.pdf   
\LAPI-Dumps\Exercise-L0-WinDbg.pdf

2023Software Diagnostics Services

For practical exercises, we need mostly needed GDB and its multiarchitecture version. If you are coming from a Windows background, you may try the same exercises with either the WinDbg app (former Windows Preview), the classical Microsoft WinDbg debugger from Debugging Tools for Windows, or even the Docker version. I decided to add WinDbg because of my recent experience with one critical software incident where GDB was not showing correct information but loading the same core dump in WinDbg helped. Here I assume you already prepared the environment and only show the multiarch GDB part, which I added for the first time to my Linux training courses.

Goal: Install GDB and check if GDB loads a core dump correctly.

Memory Analysis Patterns: Stack Trace; Incorrect Stack Trace.

1. Download core dump files if you haven’t done that already and unpack the archives:

https://www.patterndiagnostics.com/Training/LAPI/LAPI-Dumps.zip

2. Download and install the latest version of GDB. For WSL2 Debian, we used the following commands:

$\$ 1$ sudo apt install build-essential

$\$ 1$ sudo apt install gdb

To analyze A64 dumps on the same x64 host platform, we also installed multiarch GDB via:

$\$ 1$ sudo apt install gdb-multiarch

On our RHEL-type system, we installed the tools and GDB via:

$\$ 1$ sudo yum group install "Development Tools"

$\$ 1$ sudo yum install gdb

3. Verify that GDB is accessible and then exit it (q command):

$\$ 1$ gdb

GNU gdb (Debian $8 . 2 . 1 \AA - 2 + 6 3$ ) 8.2.1

Copyright (C) 2018 Free Software Foundation, Inc.

License GPLv3+: GNU GPL version 3 or later <http://gnu.org/licenses/gpl.html>

This is free software: you are free to change and redistribute it.

There is NO WARRANTY, to the extent permitted by law.

Type "show copying" and "show warranty" for details.

This GDB was configured as "x86_64-linux-gnu".

Type "show configuration" for configuration details.

For bug reporting instructions, please see:

<http://www.gnu.org/software/gdb/bugs/>.

Find the GDB manual and other documentation resources online at:

<http://www.gnu.org/software/gdb/documentation/>.

For help, type "help".

Type "apropos word" to search for commands related to "word".

(gdb) q

$\$ 1$

4. Load core.9 dump file and bash executable from the x64/ directory:

$ cd LAPI/x64

~/LAPI/x64$ gdb -c core.9 -se bash

GNU gdb (Debian $8 . 2 . 1 \AA - 2 + 6 3$ ) 8.2.1

```txt
Copyright (C) 2018 Free Software Foundation, Inc. License GPLv3+: GNU GPL version 3 or later <http://gnu.org/licenses/gpl.html> This is free software: you are free to change and redistribute it. There is NO WARRANTY, to the extent permitted by law. Type "show copying" and "show warranty" for details. This GDB was configured as "x86_64-linux-gnu". Type "show configuration" for configuration details. For bug reporting instructions, please see: <http://www.gnu.org/software/gdb/bugs/>. Find the GDB manual and other documentation resources online at: <http://www.gnu.org/software/gdb/documentation/>. For help, type "help". Type "apropos word" to search for commands related to "word"... Reading symbols from bash...(no debugging symbols found)...done. warning: core file may not match specified executable file. [New LWP 9] Core was generated by `-bash'. #0 0x00007f3e9f7492d7 in __GI__ waitpid (pid=-1, stat_location=0x7ffc61ad0, options=10) at ../sysdeps/unix/sysv/linux/waitpid.c:30 30 ../sysdeps/unix/sysv/linux/waitpid.c: No such file or directory 
```

5. Verify that the stack trace (backtrace) is shown correctly with symbols:

```txt
(gdb) bt  
#0 0x00007f3e9f7492d7 in __GI__ waitpid (pid=-1, stat_location=0x7ffc661ad0, options=10) at ../sysdeps UNIX.sysv-linux/waitpid.c:30  
#1 0x000055e1650a4869 in ??()  
#2 0x000055e1650a5cc3 in wait_for()  
#3 0x000055e165093b85 in execute_command_internal()  
#4 0x000055e165093df2 in execute_command()  
#5 0x000055e16507b833 in reader_loop()  
#6 0x000055e16507a104 in main() 
```

Note: If the stack trace on your system is incorrect, check that shared libraries are loaded, and if not, add the current directory to the shared library search path (see the A64 example below).

6. We exit GDB.

```txt
(gdb) q
~/LAPI/x64$ 
```

7. Load core.19649 dump file and bash executable from the A64/ directory using multiarch GDB:

```txt
$ cd ../A64 
```

$\sim / \mathrm{LAP1} / \mathrm{A64}$ gdb-multiarch -c core.19649 -se bash

```txt
GNU gdb (Debian 8.2.1-2+b3) 8.2.1   
Copyright (C) 2018 Free Software Foundation, Inc.   
License GPLv3+: GNU GPL version 3 or later <http://gnu.org/licenses/gpl.html>   
This is free software: you are free to change and redistribute it.   
There is NO WARRANTY, to the extent permitted by law.   
Type "show copying" and "show warranty" for details.   
This GDB was configured as "x86_64-linux-gnu".   
Type "show configuration" for configuration details. 
```

For bug reporting instructions, please see: <http://www.gnu.org/software/gdb/bugs/>. Find the GDB manual and other documentation resources online at: <http://www.gnu.org/software/gdb/documentation/>.

For help, type "help". Type "apropos word" to search for commands related to "word"... Reading symbols from bash...(no debugging symbols found)...done.

warning: core file may not match specified executable file. [New LWP 19649]

warning: Could not load shared library symbols for 3 libraries, e.g. /lib/aarch64-linuxgnu/libtinfo.so.6. Use the "info sharedlibrary" command to see the complete listing. Do you need "set solib-search-path" or "set sysroot"? Core was generated by `-bash'. #0 0x0000ffffbafa6734 in ?? ()

8. Verify that the stack trace (backtrace) is shown correctly with symbols:

(gdb) bt #0 0x0000ffffbafa6734 in ?? () #1 0x0000000000000001 in ?? () Backtrace stopped: previous frame identical to this frame (corrupt stack?)

9. If the stack trace on your system is incorrect, like in the output above, check that shared libraries are loaded, and if not, add the current directory to the shared library search path:

(gdb) info sharedlibrary From To Syms Read Shared Object Library No /lib/aarch64-linux-gnu/libtinfo.so.6 No /lib/aarch64-linux-gnu/libc.so.6 No /lib/ld-linux-aarch64.so.1

(gdb) set solib-search-path . Reading symbols from /home/coredump/LAPI/A64/libtinfo.so.6...(no debugging symbols found)...done. Reading symbols from /home/coredump/LAPI/A64/libc.so.6...(no debugging symbols found)...done. Reading symbols from /home/coredump/LAPI/A64/ld-linux-aarch64.so.1...(no debugging symbols found)...done.

(gdb) bt #0 0x0000ffffbafa6734 in wait4 () from /home/coredump/LAPI/A64/libc.so.6 #1 0x0000aaaabb7a943c in ?? () #2 0x0000aaaabb70aa50 in wait_for () #3 0x0000aaaabb6f0dac in execute_command_internal () #4 0x0000aaaabb6f160c in execute_command () #5 0x0000aaaabb6e168c in reader_loop () #6 0x0000aaaabb6d4170 in main ()

10. We exit GDB.

(gdb) q ~/LAPI/A64$

# Exercise L0 (WinDbg)

Goal: Install WinDbg or Debugging Tools for Windows, or pull Docker image, and check that symbols are set up correctly.

Patterns: Stack Trace; Incorrect Stack Trace.

1. Download memory dump files if you haven’t done that already and unpack the archives:

https://www.patterndiagnostics.com/Training/LAPI/LAPI-Dumps.zip

2. Install WinDbg (or upgrade existing WinDbg Preview) from https://learn.microsoft.com/en-gb/windowshardware/drivers/debugger. Run WinDbg.

![](images/728cbe40994cde57915c9b985f4da27e9bce20a45581994e042705faa6e3fcd6.jpg)

![](images/ffb3d241071e7ad73125777477433cd4b8de8e62e28f51e3e86c99aa26ccc0d8.jpg)

IWinDbg1.2303.30001.0

![](images/e38805db2a4f7d36b0e8377e76409f0cedcf1abb3d0d832d77fae0cbb88ae733.jpg)

![](images/eadb5ca028ccb3445d686d068a46c55811b7a416f27f1afff7ea5d5e8292e1ef.jpg)

![](images/a6e7b7bc167202af827a6881a8942068419e2f6f82457c477e884041793768c2.jpg)

![](images/1ea5cd61368df91fcee7e8a87ff9de36ae48676f802b2bef926db6e68ecf91dc.jpg)

Start debugging

Save workspace

Open source file

Open script

Settings

About

Exit

# Start debugging

![](images/45830243be48cc104a2c56eb6f1619f3f3608db4d19236ec14a5389ac4399589.jpg)

Recent

![](images/c0801be6f14e2cce61e8c1ae966f9fde691ccb8c32b739c066568d4517b0ca44.jpg)

Launch executable

![](images/8db4a0b54a6be6242f536d30a266041e1ba7d9aaf30be0cd65fcbe88b67d0283.jpg)

Launch executable (advanced)

Supports Time Travel Debugging

![](images/63e79fb9429f568c3c41b49546d3337dcaeefa621b0efcdfbd4abcafff963600.jpg)

Attach to process

Supports Time Travel Debugging

![](images/632c4c0cfc4ff385621b918e77ce8d2df0886cbf701bf367014399d44d589bc9.jpg)

Open dump file

Opens a crash dump file. (Ctrl + D)

![](images/c76c7953e92e943ebef25fd3fe08cc26f9db1004d338459b402a20f1abf504a0.jpg)

Open trace file

![](images/dcb36071b72dc83d9113d11028c6cb263918cd707ab4907129e817385755c35b.jpg)

Connect to remote debugger

![](images/d7474471c9b2b4954314c81d554ba1bcaf33fa78f2d70d69f4676d4ba026b3cb.jpg)

Connect to process server

![](images/4db5976170e2b06ce3a62e56dccf3bdbcf503226031750d0009c5b7fc991ff1f.jpg)

Attach to kernel

![](images/a12ba027d675147991e220ab78216bec68389b27adfb6abf37f006631d4bf3e7.jpg)

Launch app package

![](images/2283cd39ba25687f45d2d56f1f8ac35068facb28e46cd5a2563fdc31d096bf85.jpg)

Open workspace

You haven't debugged anything recently. Start a session with one of the options to the left.

# 4. We get the dump file loaded:

![](images/ec6c3b48a10f13ffc86c02a03be6b80c4bf7982b33b239dd18188b9bca397e48.jpg)

# 5. Type .sympath+ <path> command to set symbol path:

![](images/64e00bed566f3a6307626ef5b6399ff6b35b32eca02c8389833652a58b86686d.jpg)

# 6. Type .reload command to reload symbols:

![](images/4a3372f25e6fa96b611d7a25dbede2030f856272c2d9232df7114c37bc1b2b33.jpg)

# 7. Type k command to verify the correctness of the stack trace:

![](images/3a1ec3eb767c8dcefc7311607b53c427a8d202adef94f8a2c69fa7d3081d8e74.jpg)

![](images/39f343dbb3fbea8416479d18e5ed7934ed898f11ec6a8307d1318f2a0f62d10f.jpg)

# 8. The output of command should be this:

<table><tr><td colspan="3">0:000&gt; k</td></tr><tr><td># Child-SP</td><td>RetAddr</td><td>Call Site</td></tr><tr><td>00 0000fff`c5fd83a0</td><td>0000aaa`bb7a943c</td><td>libc(so)!wait4+0x34</td></tr><tr><td>01 0000fff`c5fd83e0</td><td>0000aaa`bb70aa50</td><td>bash!rl_enable_paren_MATCHing+0x2c0c</td></tr><tr><td>02 0000fff`c5fd8480</td><td>0000aaa`bb6f0dac</td><td>bash!wait_for+0x1c0</td></tr><tr><td>03 0000fff`c5fd8750</td><td>0000aaa`bb6f160c</td><td>bash!execute_command_internal+0x2cd8</td></tr><tr><td>04 0000fff`c5fd8880</td><td>0000aaa`bb6e168c</td><td>bash!execute_command+0xbc</td></tr><tr><td>05 0000fff`c5fd88b0</td><td>0000aaa`bb6d4170</td><td>bash!reader_loop+0x2b8</td></tr><tr><td>06 0000fff`c5fd8900</td><td>0000fff`baf173fc</td><td>bash!main+0x18b0</td></tr><tr><td>07 0000fff`c5fd8a70</td><td>0000fff`baf174cc</td><td>libc(so)!_libc_init_first+0x7c</td></tr><tr><td>08 0000fff`c5fd8b80</td><td>0000aaa`bb6d4470</td><td>libc(so)!_libc_start_main+0x98</td></tr><tr><td>09 0000fff`c5fd8be0</td><td>FFFFFF`FFFFFF</td><td>bash!start+0x30</td></tr><tr><td>0a 0000fff`c5fd8be0</td><td>00000000`00000000</td><td>0xFFFFFF`FFFFFF</td></tr></table>

If it has this form below with large offsets, then your symbol files were not set up correctly - Incorrect Stack Trace pattern:

0:000> k   
Child-SP RetAddr  
00 0000ffff`c5fd83a0 0000aaa`bb7a943c  
01 0000ffff`c5fd83e0 0000aaa`bb70aa50  
02 0000ffff`c5fd8480 0000aaa`bb6f0dac  
03 0000ffff`c5fd8750 0000aaa`bb6f160c  
04 0000ffff`c5fd8880 0000aaa`bb6e168c  
05 0000ffff`c5fd88b0 0000aaa`bb6d4170  
06 0000ffff`c5fd8900 0000ffff`baf173fc  
07 0000ffff`c5fd8a70 0000ffff`baf174cc  
08 0000ffff`c5fd8b80 0000aaa`bb6d4470  
09 0000ffff`c5fd8be0 fFFFFFF`FFFFFF  
1o a 0000ffff`c5fd8beO 0oOooOOoo'ooOooOO

Call Site   
libc(so+0xb6734
bash!rl_enable_paren_MATCHing+0x2c0c
bash!wait_for+0x1c0
bash!execute_command_internal+0x2cd8
bash!execute_command+0xbc
bash!reader_loop+0x2b8
bash!main+0x18b0
libc(so+0x273fc
libc(so+0x274cc
bash!start+0x30
0xxxxxxxx xxxx

9. [Optional] Download and install the latest version of Debugging Tools for Windows (See windbg.org for quick links, WinDbg Quick Links \ Download Debugging Tools for Windows). For this part, we use WinDbg 10.0.22621.382 from Windows 11 WDK, version 22H2.   
10. Launch WinDbg from Windows Kits \ WinDbg (X64) or Windows Kits \ WinDbg (X86). For uniformity, we use the X64 version of WinDbg throughout the exercises.

![](images/325d66fd8d39e578a528a26ee08e01369b5664084b1cb78b409d6f987c7e55f8.jpg)

![](images/cf29e12876369229eb1380343d809e5f9c22dda1f98a86afbb00a0ac7907095f.jpg)

WinDbg:10.0.22621.382 AMD64

Eile

Edit View Debug Window Help

Open Source File...

Open Executable...

Attach to a Proces...

Open Crash Dump..

Connect to Remote Session...

Connect to Remote Stub..

Kernel Debug...

Symbol File Path...

Source File Path ...

Image File Path..

Open Workspace...

Save Workspace

Save Workspace As..

Clear Workspace...

Delete Workspaces...

Open Workspace in File..

Save Workspace to File...

Map Network Drive...

Disconnect Network Drive..

Recent Files

Exit

Alt+F4

Ln O, Col 0

Sys 0:<None>

Proc 000:0

Thrd 000:0

# 12. We get the dump file loaded:

![](images/0b0bbe907f6d83a1aae636c0a7a6398675b87fdc940e5529a40ee26297eda826.jpg)

# 13. Type .sympath+ <path> command to set symbol path:

![](images/7bc14c716aa34c40081e7f7d9b2de75112bb584c44793f2e8af34a514bc112ff.jpg)

# 14. Type .reload command to reload symbols:

![](images/4539ab8e923f42829fc664561470c4340fe606380bfd46c18da1685994f4b040.jpg)

![](images/18e84160a191f0b76f08c9ba0d67c6f1fffb8e22489dd7bf08b682911eca3e1e.jpg)

![](images/eec369f11b39978e7b96b04bab436d872f4473e241b71e3194ce48aeac2f4669.jpg)

16. [Optional] If you prefer using a Docker image with WinDbg and symbol files included, follow these steps below.

c:\LAPI>docker pull patterndiagnostics/windbg:10.0.22621.382-lapi

10.0.22621.382-lapi: Pulling from patterndiagnostics/windbg

Digest: sha256:e4ccb0b18a3dacd70e7ded51ea3e6e3c15bad6b8428ed7094fe214ce0262d98e

Status: Image is up to date for patterndiagnostics/windbg:10.0.22621.382-lapi

docker.io/patterndiagnostics/windbg:10.0.22621.382-lapi

c:\LAPI>docker run -it -v C:\LAPI:C:\LAPI patterndiagnostics/windbg:10.0.22621.382-lapi

Microsoft Windows [Version 10.0.20348.1726]

(c) Microsoft Corporation. All rights reserved.

C:\WinDbg>windbg.bat C:\LAPI\A64\core.19649

Microsoft (R) Windows Debugger Version 10.0.22621.382 AMD64

Copyright (c) Microsoft Corporation. All rights reserved.

Loading Dump File [C:\LAPI\A64\core.19649]

64-bit machine not using 64-bit API

************* Path validation summary **************

Response Time (ms) Location

OK

Symbol search path is: .

Executable search path is:

Generic Unix Version 0 UP Free ARM 64-bit (AArch64)

Machine Name:

System Uptime: not available

Process Uptime: not available

*** WARNING: Unable to verify timestamp for libc.so.6

*** WARNING: Unable to verify timestamp for bash

libc_so+0xb6734:

0000ffff`bafa6734 d4000001 svc #0

0:000> .sympath+ C:\LAPI\A64

Symbol search path is: .;C:\LAPI\A64

Expanded Symbol search path is: .;c:\lapi\a64

************* Path validation summary **************

Response Time (ms) Location

OK

OK C:\LAPI\A64

0:000> .reload

*** WARNING: Unable to verify timestamp for libc.so.6

*** WARNING: Unable to verify timestamp for bash

************* Symbol Loading Error Summary **************

Module name Error

bash The system cannot find the file specified

libc.so The system cannot find the file specified

You can troubleshoot most symbol related issues by turning on symbol loading diagnostics (!sym noisy) and repeating the command that caused symbols to be loaded.

You should also verify that your symbol search path (.sympath) is correct.

```csv
0:000> k  
Child-SP RetAddr Call Site  
0000ffff`c5fd83a0 0000aaa`bb7a943c libc(so!wait4+0x34  
0000ffff`c5fd83e0 0000aaa`bb70aa50 bash!rl_enable_paren_MATCHing+0x2c0c  
0000ffff`c5fd8480 0000aaa`bb6f0dac bash!wait_for+0x1c0  
0000ffff`c5fd8750 0000aaa`bb6f160c bash!execute_command_internal+0x2cd8  
0000ffff`c5fd8880 0000aaa`bb6e168c bash!execute_command+0xbc  
0000ffff`c5fd88b0 0000aaa`bb6d4170 bash!reader_loop+0x2b8  
0000ffff`c5fd8900 0000ffff`baf173fc bash!main+0x18b0  
0000ffff`c5fd8a70 0000fff`baf174cc libc(so!_libc_init_first+0x7c  
0000ffff`c5fd8b80 0000aaa`bb6d4470 libc.so!_libc_start_main+0x98  
0000ffff`c5fd8beo fFFFFFF `FFFFFF bash!start+0x30  
0000ffff`c5fd8beo 0000ooo`ooOOOOoo 0xFFFFFF 
```

```batch
0:000> q quit: NatVis script unloaded from 'C:\Program Files\Windows Kits\10\Debuggers\x64\Visualizers\gstl.natvis' 
```

```batch
C:\WinDbg>exit  
c:\LAPI> 
```

# Why Linux API?

Development   
Malware analysis   
Vulnerability analysis and exploitation   
Reversing   
Diagnostics   
Debugging   
Monitoring   
Memory forensics   
Crash and hang analysis   
Secure coding   
Static code analysis   
Trace and log analysis

2023Software Diagnostics Services

First, why did we create this course? The knowledge of Linux API is necessary for many software construction and post-construction activities listed on this slide. In this training, we look at Linux API from a software diagnostics perspective. This perspective includes core memory dump analysis and, partially, trace and log analysis. The knowledge of Linux API is tacitly assumed in my other courses. Course attendees sometimes ask why I chose a particular function from a backtrace for analysis or what this or that function is doing. Of course, there is an intersection of what we learn with other areas as well. During this course, we also do a bit of live debugging.

# My History of Linux API

C runtime library from 1989   
） I started using Linux APl in 1997 (Old CV)   
Commercial application development in 20o0 - 2003   
Reading of Linux APl for core dump analysis since 2015   
Teaching foundations of x64 Linux debugging from 2021   
and A64 Linux debugging from 2022, two books   
③ System programming using Linux APl in 2022   
Linux disassembly and reversing course in 2022,   
second edition in 2023   
Third edition of Linux core dump analysis course in 2023   
） Commercial core dump analysis service in 2023

2023Software Diagnostics Services

It is hard for me to recall when I started using Linux API. It is a distant memory but not as distant as Windows API. I believe it was the mid-1990s when I at least installed Linux a few times. Fortunately, I recalled that I meticulously documented my work history before 2003 in my old CV, which I no longer maintain. So, from the record, I see I started using Linux API in 1997. In 2000, I designed and partly implemented a multiplatform application for Windows, Linux, and Solaris. Linux was my primary development system for $\mathsf { C } { + } { + }$ static code analysis tools in 2001 – 2003, with Emacs as the code editor and GNU tool chain for development. Then in mid-2003, I switched completely to Windows development. In 2012 I started working on macOS (at that time called Mac OS X) core dump analysis after a brief encounter with FreeBSD internals and published the course and its second edition in 2014 with added LLDB material. Then in 2015, I started doing Linux core dump analysis and published the first edition of the course. In 2017 – 2020, I studied Linux internals while preparing for various interviews. Finally, in mid-2020, I switched to full-time Linux-based cloud development. Throughout 2022, I used Linux API daily to implement Linux API monitoring tools. In 2023 I continued updating various Linux courses and even started commercial Linux core dump analysis with the success of diagnosing customer issues and providing recommendations.

Old CV https://opentask.com/Vostokov/CV.htm

# Perspectives of Linux APl

Memory analysis: dumps /live debugging   
Disassembly, reconstruction, reversing   
Trace and log analysis (strace, Itrace, perf, eBPF, procmon)   
Category theory

2023Software Diagnostics Services

The perspective we take is from software diagnostics, particularly memory analysis (this may include memory forensics), which includes both dump analysis and live debugging, disassembly, reconstruction, reversing (the so-called ADDR patterns), and trace and log analysis using tools such as strace, ltrace, perf, eBPF, and even procmon for Linux. We do not look at Linux API from a development perspective, such as writing applications and services, although there is some necessary intersection, especially when doing diagnostics and vulnerability analysis. We also take a view of Linux API from a category theory perspective and compare it to Windows API as well.

# Procmon

https://github.com/Sysinternals/ProcMon-for-Linux

# What Linux API?

Source code perspective   
ABl (Application Binary Interface) perspective

Libraries   
Syscalls

2023Software Diagnostics Services

Let’s determine what we mean by Linux API. There are two general perspectives on this question. The first is the source code (or software construction) perspective. In this training, we take a different software post-construction perspective: what is seen from already compiled and linked modules is what we call Linux API.

# APl and Language Levels

Conceptual cross-platform level   
High-level and assembly language   
Machine language

2023Software Diagnostics Services

Conceptually, there are many similarities between different APIs, such as Windows and Linux. This is understandable since a large part of API centers on interfacing with OS, which must provide common resource abstractions. The next level is the high-level and assembly languages level, which differs across platforms. And finally, the machine language level erases all differences in the previous level.

x64 and A64

# x64 and A64

2023Software Diagnostics Services

In this section, we provide an overview of disassembly for x64 and ARM64 platforms. Windows developers who know x64 assembly language may benefit because we use a different flavor, default in Linux GDB. Compared to other Linux courses where I added the same review, it has slightly less material since we are not interested in stack trace reconstruction, for example. However, I added a small number of additional examples we see in this course’s core dumps.

# CPU Registers (x64)

RAX > EAX → AX ≌ {AH, AL}

RAX 64-bit

EAX 32-bit

ALU: RAX, RdX   
Counter: RCX   
Memory copy: RSI (src), RDI (dst)   
Stack: RSP, RBP   
Next instruction: RIP   
New: R8 - R15, Rx(D|W/L)

2023Software Diagnostics Services

There are familiar 32-bit CPU register names, such as EAX, that are extended to 64-bit names, such as RAX. Most of them are traditionally specialized, such as ALU, counter, and memory copy registers. Although, now they all can be used as general-purpose registers. There is, of course, a stack pointer, RSP, and, additionally, a frame pointer, RBP, that is used to address local variables and saved parameters. It can be used for backtrace reconstruction. In some compiler code generation implementations, RBP is also used as a general-purpose register, with RSP taking the role of a frame pointer. An instruction pointer RIP is saved in the stack memory region with every function call, then restored on return from the called function. In addition, the x64 platform features another eight general-purpose registers, from R8 to R15.

# Instructions: registers (x64)

Opcode SRC，DST # default AT&T flavour   
Examples:

```asm
mov $0x10, %rax # 0x10 → RAX
mov %rsp, %rbp # RSP → RBP
add $0x10, %r10 # R10 + 0x10 → R10
imul %ecx, %ecx # ECX * EDX → EDX
callq %%rdx # RDX already contains
# the address of func (&func)
# PUSH RIP; &func → RIP
sub $0x30, %rsp # RSP-0x30 → RSP
# make a room for local variables 
```

2023Software Diagnostics Services

This slide shows a few examples of CPU instructions involving operations with registers, such as moving a value and doing arithmetic. The direction of operands is opposite to the Intel x64 disassembly flavor if you are accustomed to WinDbg on Windows. It is possible to use the Intel disassembly flavor in GDB, but we opted for the default AT&T flavor in line with our Accelerated Linux Core Dump Analysis book.

# Memory and Stack Addressing

Lower addresses

![](images/09db2d69436f5bf14fea409d065d23c9f78149b9399734f0142739931b3e077d.jpg)  
Higher addresses

2023Software Diagnostics Services

Before we look at operations with memory, let’s look at a graphical representation of memory addressing. A thread stack is just any other memory region, so instead of RSP and RBP, any other register can be used. Please note that stack grows towards lower addresses, so to access the previously pushed values, you need to use positive offsets from RSP.

# Instructions: memory load (x64)

Opcode Offset(SRC)， DST  
Opcode DST   
Examples:

mov 0x10(%rsp), %rax #value at address RSP+0x10 $\rightarrow$ RAX  
mov -0x10(%rbp), %rcx #value at address RBP-0x10 $\rightarrow$ RCX  
add (%rax), %rdx #RDX + value at address RAX $\rightarrow$ RDX  
pop %rdi #value at address RSP $\rightarrow$ RDI  
#RSP + 8 $\rightarrow$ RSP  
lea 0x20(%rbp), %r8 #address RBP+0x20 $\rightarrow$ R8

2023Software Diagnostics Services

Constants are encoded in instructions, but if we need arbitrary values, we must get them from memory. Round brackets show memory access relative to an address stored in some register.

# Instructions: memory store (x64)

Opcode SRC，Offset(DST)   
Opcode SRC|DST   
Examples:

mov %rcx, -0x20(%rbp) # RCX $\rightarrow$ value at address RBP-0x20  
add1 $1, (%rax) # 1 + 32-bit value at address RAX  
push %rsi # RSP - 8 $\rightarrow$ RSP  
inc (%rcx) # 1 + value at address RCX  
# value at address RCX

2023Software Diagnostics Services

Storing is similar to loading.

# Instructions: flow (x64)

Opcode DST   
Examples:

jmpq 0x10493fc1c

# 0x10493fc1c RIP

# (goto 0x10493fc1c)

jmpq *0x100(%rip)

# value at address RIP+0x100 RIP

callq 0x10493ff74

# RSP - 8 → RSP

0x10493fc14:

# 0x10493fc14 → value at address RSP

# 0x10493ff74 → RIP

# (goto 0x10493ff74)

2023Software Diagnostics Services

Goto (an unconditional jump) is implemented via the JMP instruction. Function calls are implemented via CALL instruction. For conditional branches, please look at the official Intel documentation. We don’t use these instructions in our exercises.

# Function Call and Prolog (x64)

![](images/8d66d51548269a2ea9ad30e8601c3c594d71628a007398c66bbaf1647ee61941.jpg)

2023Software Diagnostics Services

When a function is called from the caller, a callee needs to do certain operations to make room for local variables on the thread stack. There are different ways to do that, and the assembly language code on the left is one of them. I use a different color in the diagram on the right to highlight the updated RSP and RBP values from the start of the proc function up to the moment when the proc2 function is called. For simplicity of illustration, I only use 64-bit values.

# CPU Registers (A64)

X0 - X28, W0-W28   
X16 (XIPO), X17 (XIP1)   
Stack: SP, X29 (FP)   
Next instruction: PC   
Link register: X30 (LR)   
Zero register: XZR, WZR   
64-bit floating point registers D0 - D31   
128-bit Q0 - Q31

2023Software Diagnostics Services

There are 31 general registers from X0 and X30, with some delegated to specific tasks such as intraprocedure calls (X16, XIP0, and X17, XIP1), addressing stack frames (Frame Pointer, FP, X29) and return addresses, the so-called Link Register (LR, X30). When you call a function, the return address of a caller is saved in LR, not on the stack as in Intel/AMD x64. The return instruction in a callee uses the address in LR to assign it to PC and resume execution. But if a callee calls other functions, the current LR needs to be manually saved somewhere, usually on the stack. There’s Stack Pointer, SP, of course. To get zero values, there’s the so-called Zero Register, XZR. All X registers are 64-bit, and 32- bit lower parts are addressed via the W prefix. There are also 128-bit SIMD registers. Next, we briefly look at some aspects related to our exercises.

# Instructions: registers (A64)

Opcode DST，SRC，SRC2   
Examples:

mov x0, #16 // X0 $\leftarrow$ 16 (0x10)  
mov x29, sp // X29 $\leftarrow$ SP  
add x1, x2, #16 // X1 $\leftarrow$ X2+16 (0x10)  
mul x1, x2, x3 // X1 $\leftarrow$ X2*X3  
blr x8 // X8 already contains // the address of func (&func) // LR $\leftarrow$ PC+4; PC $\leftarrow$ &func  
sub sp, sp, #48 // SP $\leftarrow$ SP-48 (-0x30) // make a room for local variables

2023Software Diagnostics Services

This slide shows a few examples of CPU instructions that involve operations with registers, for example, moving a value and doing arithmetic. The direction of operands is the same as in the Intel x64 disassembly flavor if you are accustomed to WinDbg on Windows. It is equivalent to an assignment. BLR is a call of some function whose address is in the register. BL means Branch and Link.

# Memory and Stack Addressing

![](images/41d7eb3fda9c54f6b1afbb37f6c575f2f3a433a9de971fc440d727be98fa7f6c.jpg)

2023Software Diagnostics Services

Before we look at operations with memory, let's look at a graphical representation of memory addressing. A thread stack is just any other memory region, so instead of SP and X29 (FP), any other register can be used. Please note that the stack grows towards lower addresses, so to access the previously pushed values, you need to use positive offsets from SP.

# Instructions: memory load (A64)

Opcode DST，DST2，[SRC，Offset]   
O Opcode DS DS [SRC]，Offset // Postincrement 2   
Examples:

```asm
ldr x0, [sp] // X0 < value at address SP+0  
ldr x0, [x29, #-8] // X0 < value at address X29-0x8  
ldp x29, x30, [sp, #32] // X29 < value at address SP+32 (0x20)  
ldp x29, x30, [sp], #16 // X30 < value at address SP+40 (0x28)  
ldp x29, x30, [sp], #16 // X29 < value at address SP+0  
ldp x29, x30, [sp], #16 // X30 < value at address SP+8  
// SP < SP+16 (0x10) 
```

2023Software Diagnostics Services

Constants are encoded in instructions, but if we need arbitrary values, we must get them from memory. Square brackets are used to show memory access relative to an address stored in some register. There’s also an option to adjust the value of the register after load, the so-called Postincrement, which can be negative. As we see later, loading pairs of registers can be useful.

# Instructions: memory store (A64)

Opcode SRC， SRC2, [DST，Offset]  
Opcode SRC， SRC2, [DST Offset]! Preincrement  
Examples:

str x0，[sp，#16] // x0 → value at address SP+16 (0x10)

str x0，[x29. #-8] // x0 → value at address X29-8

stp x29，x30，[sp，#32] // x29 → value at address SP+32 (0x20)

// x30 → value at address SP+40 (0x28)

stp x29，x30，[sp,#-16]！ // SP ← SP-16 (-0x10)

// x29 → set value at address SP

// x30 → set value at address SP+8

2023Software Diagnostics Services

Storing operand order goes in the other direction compared to other instructions. There’s a possibility to Preincrement the destination register before storing values.

# Instructions: flow (A64)

③ Opcode DST，SRC   
Examples:

adrp x0,0x420000 // x0 ← 0x420000

b 0x10493fc1c // PC ←/0x10493fc1c

// (goto 0x10493fcic)

brx17 // PC ← the value of X17

0x10493fc14:  // PC == 0x10493fc14

b10x10493ff74 // LR ← PC+4 (0x10493fc18)

// PC ← 0x10493ff74

// (goto 0x10493ff74)

2023Software Diagnostics Services

Because the size of every instruction is 4 bytes (32 bits), it is only possible to encode a part of a large 4GB address range, either as a relative offset to the current PC or via ADRP instruction. Goto (an unconditional branch) is implemented via the B instruction. Function calls are implemented via the BL (Branch and Link) instruction.

# Function Call and Prolog (A64)

![](images/12b59596d2eb714855c65f75e6ac37783b243842cbd138b8660f8a9f897d1efa.jpg)

2023Software Diagnostics Services

When a function is called from the caller, a callee needs to do certain operations to make room for local variables on the thread stack and save LR if there are further calls in the function body. There are different ways to do that, and the assembly language code on the left is one of them GCC compiler uses by default. I use a different color in the diagram on the right to highlight the updated SP and X29 (FP) values from the start of the proc function up to the moment when the proc2 function is called. Please also note an example of zero register usage and the fact that both SP and X29 point to the same memory location. Positive offsets from X29 are used to address local variables. For simplicity of illustration, I only use 64-bit values.

# General Linux API Aspects

# General Linux APl Aspects

2023 Software Diagnostics Services

# Lexicon

IAT ←→ PLT   
DLL ←→SO   
Module ←→ Shared Library

2023Software Diagnostics Services

On this slide, I keep adding some vocabulary that differs between the standard accounts of Windows and Linux APIs but refers to the same concepts. IAT is Import Address Table, PLT is Procedure Linkage Table.

# Manual Page and Header Views

Manual pages (2 and 3)

Syscalls /overview   
. Library calls

Headers

Kernel source code cross-reference   
Manual pages 0

2023Software Diagnostics Services

If you are unfamiliar with C or $\mathsf { C } { + } { + }$ , a header is a textual file referenced in source code and inserted during compilation. It can contain anything but most likely a programming language source code such as function declarations, in our context, for example, declarations of Linux API functions and types. So, for example, if we are interested in creating threads, the pthread_create API function is declared in the pthread.h header file.

# Syscalls

https://man7.org/linux/man-pages/dir_section_2.html

# Overview

https://man7.org/linux/man-pages/man2/syscalls.2.html

# Kernel source code cross-reference

https://elixir.bootlin.com/linux/latest/source

# Manual page 0

https://man7.org/linux/man-pages/man0/

# Naming Convention

Naming conventions   
Functions, types, parameters, fields: twowords snake case

pthread_create   
pid_t   
tm_sec

Constants, maCrOS: TWOWORDS，SCREAMING_SNAKE_CASE

SOCK_STREAM

2023Software Diagnostics Services

There are several naming conventions. The Linux API convention differs significantly from the Windows API convention; also, there are differences between different programming languages; please see the first link on the slide. Linux API naming connection is close to Standard C and $\mathsf { C } { + } { + }$ library naming conventions. Linux API uses twowords and snake_case for functions, types, parameters, and structure fields. Constants and macros use a different snake case convention called SCREAMING_SNAKE_CASE and also TWOWORDS.

Naming conventions

https://en.wikipedia.org/wiki/Naming_convention_(programming)

Snake Case

https://en.wikipedia.org/wiki/Snake_case

# Basic Type System

Basic types are typedef-ed   
inttypes.h

uint64_t

sys/types.h   
. size_t,pid_t

GDB Commands

(gdb) info types

(gdb) info functions

# WinDbg Commands

0:000> dt *!*

0:000>× *!*

2023Software Diagnostics Services

Linux API uses a C language type system with additional custom types usually typedef-ed from standard C types. Basic types are defined in a few files. Examples are shown on this slide.

# Call Types (x64)

Direct (same executable or shared library)

```asm
shell_execve:  
...  
0x000055e16508f807 <=103>: callq 0x55e1650891a0 <file_isdir> 
```

Indirect

Pointer (.got.plt)

PLT inter-module call

```txt
shell_execute:  
...  
0x000055e16508f89b <=251>: callq 0x55e165078aa0 <open@plt>  
open@plt:  
...  
0x000055e165078aa0 <=0>: jmpq *0xe7aaa(%rip) # 0x55e165160550 <open@got.plt> 
```

2023Software Diagnostics Services

Now we come from source code to implementation details. Linux API calls are ultimately implemented as call instructions on Intel and branch and link instructions on ARM platforms. In our training, we show examples in Exercises for x64 and ARM64. The compiler chooses different variants of call instructions based on whether the destination is in the same module or not. Direct calls may be emitted when a callee in the same module, a relative offset to the current instruction pointer is known, or an address is absolute.

# Call Types (A64)

③ Direct (same executable or shared library)

```txt
shell_execve: 
```

```txt
# 
```

```xml
0x0000aaaabb6f71e4 <+160>: b1 0xaaabb742530 <file_status> 
```

Indirect

Pointer (.got)

○ PLT inter-module call

```asm
shell_execute:  
...  
0x0000aaaabb6f7210 <+204>: bl 0xaaabb6d1ec0 <open@plt>  
open@plt:  
...  
0x0000aaaabb6d1ec0 <+0>: adr p x16, 0xaaabb7ff000  
0x0000aaaabb6d1ec4 <+4>: ldr x17, [x16, #1464]  
...  
0x0000aaaabb6d1ecc <+12>: br x17 
```

2023Software Diagnostics Services

In ARM64, we see a similar picture where GOT (Global Offset Table) is used. We also see an example of intra-procedure registers usage (X16 and X17).

# APl as Interface

Provided by (exported from) some .so library   
Used by (imported by) executable or .so   
Can be functional or object-oriented

![](images/65d5b8b6359e42d3553d93e2d080442b2e4334038b473de4bc19d8099c36ee84.jpg)

2023 Software Diagnostics Services

Most of the time, Linux API means a set of interfaces provided by or exported from some Shared Library. These interfaces are used by or imported by some other Shared Library or executable. Interfaces can be functional or object-oriented conceptually. Let’s look at how these API interfaces are implemented.

# Exploration Tools

1dd   
readelf   
objdump   
$ LD DEBUG=libs bash

2023Software Diagnostics Services

In addition to tracing and GDB, there are several exploration tools for shared library interfaces and dependencies that we will use in some exercises.

ldd

https://man7.org/linux/man-pages/man1/ldd.1.html

readelf

https://man7.org/linux/man-pages/man1/readelf.1.html

objdump

https://man7.org/linux/man-pages/man1/objdump.1.html

LD_DEBUG

https://man7.org/linux/man-pages/man8/ld.so.8.html

# Symbol Import/Export

![](images/ab56947b6ebfa6063ab234f4d9baf31b4f14bfe40b34e50c66a8c68d557c9659.jpg)

2023Software Diagnostics Services

A shared library that wants to provide an interface has the so-called .dynsym section of names and corresponding locations in code. On the other side, if some shared library or executable file, wants to use some names from that interface, it also has the so-called .rela.plt section that references its own .dynsym section that contains these names, as illustrated on this slide where app wants to use open from libc.so.6.

# Procedure Linkage Table (x64)

![](images/97d25b7e75ec6b063fafe0286a473cbd440910f308d77e03060d551836eb9f97.jpg)

2023Software Diagnostics Services

When a shared library or executable that wants to use interfaces from other shared libraries is loaded, the dynamic linker modifies the so-called .GOT.PLT section by populating each entry there with the real addresses from other already loaded shared libraries that export the required interface entries. For example, if the app wanted to use the open function from libc.so.6, the corresponding entry in .GOT.PLT will be filled with the real address of that function inside the libc.so.6 code. If we want to call an interface entry, indirect addressing is used to transfer execution to the interface shared library code. First, it calls an entry in .PLT stub and then jumps indirectly to an address from .GOT.PLT that points to code in another shared library that implements the interface entry. This indirection allows calling interfaces independently from their implementation code location in memory since the relative address of .PLT is the same. This mechanism also allows shared libraries to be loaded at different addresses during each program run, as illustrated in the next slide. The reason why two sections .PLT and .GOT.PLT are used will be clear when we look at dynamic linking later in this course.

# Procedure Linkage Table (A64)

![](images/4af47d45e3761fc4f733b0e78fb9e3f5943de022eafb9bb4a685abe977487057.jpg)

2023Software Diagnostics Services

The picture for ARM64 is similar, except that section names are slightly different.

# Virtual Process Address Space

![](images/73de6941785fa3da0e0413c7f7bd8257ccb21cf54bc1d73cb79a832d90d73c97.jpg)

![](images/e14ac5002f9c77e0bd4ae6882645342dede4d96fe40491ab6324b473352cc788.jpg)

2023Software Diagnostics Services

This slide shows different program executions where, each time, the same shared library is loaded at different addresses. We see that indirect addressing allows the app code to call the libc code loaded at different addresses in virtual memory.

# Exercise L1

Goal: Explore import/export information and calls to shared libraries   
ADDR Patterns: Call Path   
\LAPI-Dumps\Exercise-L1-GDB.pdf   
\LAPI-Dumps\Exercise-L1-WinDbg.pdf

2023Software Diagnostics Services

Goal: Explore import/export information and calls to shared libraries.

ADDR Patterns: Call Path.

1. Disassemble the execve@plt function from the bash executable in the x64 directory:

```txt
~/LAPI/x64$ objdump -d bash | grep -A 4 "<execve@plt>:"  
000000000002d5e0 <execve@plt>:  
2d5e0: ff 25 0a 7d 0e 00 jmpq *0xe7d0a(%rip) # 1152f0 <execve@GLIBC_2.2.5>  
2d5e6: 68 5b 00 00 00 pushq $0x5b  
2d5eb: e9 30 fa ff ff jmpq 2d020 <endgrent@plt-0x10> 
```

2. Dump all ELF info from the bash executable in the x64 directory and look for the 1152f0 entry in the .rela.plt section and the referenced 94 (0x5e) entry in the .dynsym section:

```asm
~/LAPI/x64$ readelf -a bash
[...] Relocation section '.rela.plt' at offset 0x2b928 contains 218 entries:
Offset Info Type Sym. Value Sym. Name + Addend
[...] 0000001152f0 005e0000007 R_X86_64_JUMP_SLO 000000000000000 execve@GLIBC_2.2.5 + 0
[...] Symbol table '.dynsym' contains 2432 entries:
Num: Value Size Type Bind Vis Ndx Name
[...] 94: 000000000000000 0 FUNC GLOBAL DEFAULT UND execve@GLIBC_2.2.5 (2)
[...] 
```

3. Dump all ELF info from the libc.so.6 shared library in the x64 directory and look for the execve@GLIBC function in the .dynsym section:

```txt
~/LAPI/x64$ readelf -a libc.so.6
[...] 
Symbol table '.dynsym' contains 2359 entries:
Num: Value Size Type Bind Vis Ndx Name
[...] 
1508: 0000000000c68a0 33 FUNC WEAK DEFAULT 13 execve@@GLIBC_2.2.5 
```

4. Load a core dump core.9 and bash executable from the x64 directory:

```txt
~/LAPI/x64$ gdb -c core.9 -se bash
GNU gdb (Debian 8.2.1-2+b3) 8.2.1
Copyright (C) 2018 Free Software Foundation, Inc.
License GPLv3+: GNU GPL version 3 or later <http://gnu.org/licenses/gpl.html>
This is free software: you are free to change and redistribute it.
There is NO WARRANTY, to the extent permitted by law.
Type "show copying" and "show warranty" for details.
This GDB was configured as "x86_64-linux-gnu".
Type "show configuration" for configuration details.
For bug reporting instructions, please see:
<http://www.gnu.org/software/gdb/bugs/>. 
Find the GDB manual and other documentation resources online at:
<http://www.gnu.org/software/gdb/documentation/>. 
```

```txt
For help, type "help".  
Type "apropos word" to search for commands related to "word"...  
Reading symbols from bash...(no debugging symbols found)...done.  
warning: core file may not match specified executable file.  
[New LWP 9]  
Core was generated by `-bash`.  
#0 0x00007f3e9f7492d7 in __GI__waitpid (pid=-1, stat_location=0x7ffc661ad0, options=10) at ../sysdeps/linux/sysv/linux/waitpid.c:30  
30 ../sysdeps/linux/sysv/linux/waitpid.c: No such file or directory. 
```

5. Set logging to a file in case of lengthy output from some commands:

```txt
(gdb) set logging on L1.log Copying output to L1.log. 
```

6. Disassemble the shell_execve function and find the call to the open@plt function:

```txt
(gdb) disassemble shell_execute
Dump of assembler code for function shell_execute:
0x000055e16508f7a0 <=0>: push %r15
0x000055e16508f7a2 <=2>: push %r14
0x000055e16508f7a4 <=4>: mov %rdx,%r14
0x000055e16508f7a7 <=7>: push %r13
0x000055e16508f7a9 <=9>: mov %rsi,%r13
0x000055e16508f7ac <=12>: push %r12
0x000055e16508f7ae <=14>: push %rbp
0x000055e16508f7af <=15>: push %rbx
0x000055e16508f7b0 <=16>: mov %rdi,%rbx
0x000055e16508f7b3 <=19>: sub $0xa8,%rsp
0x000055e16508f7ba <=26>: mov %fs:0x28,%rax
0x000055e16508f7c3 <=35>: mov %rax,0x98(%rsp)
0x000055e16508f7cb <=43>: xor %eax,%eax
0x000055e16508f7cd <=45>: callq 0x55e1650785e0 <execve@plt>
0x000055e16508f7d2 <=50>: callq 0x55e165078120 <__errno_location@plt>
0x000055e16508f7d7 <=55>: mov %rax,%r12
0x000055e16508f7da <=58>: mov (%rax),%ebp
0x000055e16508f7dc <=60>: mov 0xdc2a6(%rip),%eax # 0x55e16516ba88<terminating_signal>
0x000055e16508f7e2 <=66>: test %eax,%eax
0x000055e16508f7e4 <=68>: jne 0x55e16508f880 <shell_execute+224>
0x000055e16508f7ea <=74>: cmp $0x8,%ebp
0x000055e16508f7ed <=77>: je 0x55e16508f894 <shell_execute+244>
0x000055e16508f7f3 <=83>: xor %eax,%eax
0x000055e16508f7f5 <=85>: cmp $0x2,%ebp
0x000055e16508f7f8 <=88>: mov %rbx,%rdi
0x000055e16508f7fb <=91>: sete %al
0x000055e16508f7fe <=94>: add $0x7e,%eax
0x000055e16508f8o1 <=97>: mov %eax, 0xde321(%rip) # 0x55e16516db28<last_command_exit_value>
\( \begin{array}{l} \texttt{O} \times \texttt{O} \times \texttt{O} \times \texttt{O} \times \texttt{O} \times \texttt{O} \times \texttt{O} \times \texttt{O} \times \texttt{O} \times \texttt{O} \times \texttt{O} \times \texttt{O} \times \texttt{O} \times \texttt{\_} \\ \texttt{O} \times \texttt{O} \times \texttt{O} \times \texttt{O} \times \texttt{O} \times \texttt{O} \times \texttt{O} \times \texttt{O} \times \texttt{O} \times \texttt{O} \times \texttt{O} \times \texttt{O} \times \texttt{o} \\ \texttt{O} \times \texttt{O} \times \texttt{O} \times \texttt{O} \times \texttt{O} \times \texttt{O} \times \texttt{O} \times \texttt{O} \times \texttt{O} \times \texttt{o} \\ \texttt{O} \times \texttt{O} \times \texttt{O}\texttt{O}\texttt{O}\texttt{O}\texttt{E}\texttt{i}\texttt{s}\texttt{o}\texttt{f}\texttt{i}\texttt{a}\texttt{o}\\ \texttt{O} \times \texttt{O}\texttt{O}\texttt{O}\texttt{O}\texttt{E}\texttt{i}\texttt{s}\texttt{o}\texttt{f}\texttt{i}\texttt{o}\\ \texttt{O} \times \texttt{O}\texttt{O}\texttt{O}\texttt{E}\texttt{i}\texttt{o}\texttt{s}\texttt{o}\texttt{o}\texttt{i}\texttt{o}\\ \texttt{O} \times \texttt{O}\texttt{O}\texttt{O}\texttt{i}\texttt{o}\texttt{s}\texttt{o}\texttt{i}\texttt{o}\\ \texttt{O} \times \texttt{O}\texttt{O}\texttt{i}\texttt{o}\texttt{o}\texttt{i}\texttt{o}\\ \texttt{O} \times \texttt{O}\texttt{o}\texttt{o}\texttt{i}\texttt{o}\texttt{o}\texttt{i}\texttt{o}\\ \texttt{O} \times \texttt{o}\texttt{o}\texttt{o}\texttt{i}\texttt{o}\texttt{o}\texttt{i}\texttt{o}\\ \texttt{O} \times \texttt{o}\texttt{o}\texttt{o}\texttt{i}\texttt{o}\texttt{o}\texttt{i}\texttt{o}\\ \texttt{O} \times \texttt{o}\texttt{o}\texttt{o}\texttt{i}\texttt{o}\mathrm{\_} \\ \mathrm{\_} \\ {\mathrm{\_}}\\ {\mathrm{\_}}\\ {\mathrm{\_}}\\ {\mathrm{\_}}\\ {\mathrm{\_}}\\ {\mathrm{\_}}\\ {\mathrm{\_}}\\ {\mathrm{\_}}\\ {\mathrm{\_}}\\ {\mathrm{\_}}\\ {\mathrm{\_}}\\ {\mathrm{\_}}\\ {\mathrm{\_}}\\ {\mathrm{\_}}\\ {\mathrm{\_}}\\ {\mathrm{\_}}\\ {\mathrm{\_}}\\ {\mathrm{\_\}}\\ {\mathrm{\_\}}\\ {\mathrm{\_\}}\\ {\mathrm{\_\}}\\ {\mathrm{\_\}}\\ {\mathrm{\_\}}\\ {\mathrm{\_\}}\\ {\mathrm{\_\}}\\ {\mathrm{\_\}}\\ {\mathrm{\_\}}\\ {\mathrm{\_\}}\\ {\mathrm{\_\}}\\ {\mathrm{\_\}}\\ {\mathrm{\_\}}\\ {\mathrm{\_\}}\\ {\mathrm{\_\}}\\ {\mathrm{\_\}}\\ {\it last\_command\_exit\_value>} \\ { } \\ { } \\ { } \\ { } \\ { } \\ { } \\ { } \\ { } \\ { } \\ { } \\ { } \\ { } \\ { } \\ { } \\ { } \\ { } \\ { } \\ { } \\ { } \\ { } \\ { } \\ { } \\ { } \\ { } \\ { } \\ { } \\ { } \\ { } \\ { } \\ { } \\ { } \\ { } \\ { } \\ { } \\
\begin{array}{l}
\begin{array}{l}
\begin{array}{l}
\begin{array}{l}
\begin{array}{l}
\begin{array}{l}
\begin{array}{l}
\begin{array}{l}
\begin{array}{l}
\begin{array}{l}
\begin{array}{l}
\begin{array}{l}
\begin{array}{l}
\begin{array}{l}
\begin{array}{l}
\end{array}
\end{array}
\end{array}
\end{array}
\end{array}
\end{array}
\end{array}
\end{array}
\end{array}
\end{array}
\end{array}
\end{array}
\end{array}
\end{array}
\end{array}
\end{array}
\end{array}
\end{array}
\end{array}
\end{array}
\end{ 
```

```asm
0x000055e16508f825 <+133>: cmp $0xc,%ebp
0x000055e16508f828 <+136>: je 0x55e16508f841 <shell_execve+161>
0x000055e16508f82a <+138>: xor %esi,%esi
0x000055e16508f82c <+140>: mov %rbx,%rdi
0x000055e16508f82f <+143>: xor %eax,%eax
0x000055e16508f831 <+145>: callq 0x55e165078aa0 <open@plt>
0x000055e16508f836 <+150>: mov %eax,%r13d
--Type <RET> for more, q to quit, c to continue without paging--q
Quit 
```

7. Follow the call path and indirect jump to see the real call from the libc.so.6 library:

```txt
(gdb) disassemble 0x55e165078aa0  
Dump of assembler code for function open@plt:  
0x000055e165078aa0 <=0>: jmpq *0xe7aaa(%rip) # 0x55e165160550 <open@got.plt>  
0x000055e165078aa6 <=6>: pushq $0xa7  
0x000055e165078aab <=11>: jmpq 0x55e165078020  
End of assembler dump. 
```

```txt
(gdb) x/a 0x55e165160550  
0x55e165160550 <open@got.plt>: 0x7f3e9f76d010 <__libc_open64> 
```

```txt
(gdb) info sharedlibrary  
From To Syms Read Shared Object Library  
0x00007f3e9f856950 0x00007f3e9f863dc8 Yes (*) /lib/x86_64-linux-gnu/libtinfo.so.6  
0x00007f3e9f844130 0x00007f3e9f844eb5 Yes /lib/x86_64-linux-gnu/libdl.so.2  
0x00007f3e9f6a5320 0x00007f3e9f7eb14b Yes /lib/x86_64-linux-gnu/libc.so.6  
0x00007f3e9f884090 0x00007f3e9f8a1b50 Yes /lib64/ld-linux-x86-64.so.2  
0x00007f3e9f37e300 0x00007f3e9f384578 Yes /lib/x86_64-linux-gnu/libnss_files.so.2  
(*): Shared library is missing debugging information. 
```

8. We can also check the section for the address 0x55e165160550:

(gdb) info file   
Symbols from "/home/coredump/LAPI/x64/bash".   
Local core dump file: /home/coredump/LAPI/x64/core.9', file type elf64-x86-64. 0x000055e16515d000 - 0x000055e165160000 is load1 0x000055e165160000 - 0x000055e165169000 is load2 0x000055e165169000 - 0x000055e165173000 is load3 0x000055e1667f5000 - 0x000055e166879000 is load4 0x00007f3e9f388000 - 0x00007f3e9f389000 is load5 0x00007f3e9f389000 - 0x00007f3e9f38a000 is load6 0x00007f3e9f38a000 - 0x00007f3e9f390000 is load7 0x00007f3e9f680000 - 0x00007f3e9f683000 is load8 0x00007f3e9f839000 - 0x00007f3e9f83d000 is load9 0x00007f3e9f83d2D - 0x0oOoO7f3e9f83fFOOO is load1o 0xOoOoO7f3e9f83fOOO - 0xOoOoO7f3e9f843Doo is load11 0xOoOoO7f3e9f846Doo - 0xOoOoO7f3e9f847Doo o is load12 0xOoOoO7f3e9f847Doo - 0xOoOoO7f3e9f848Doo is load13 0xOoOoO7f3e9f871Doo - 0xOoOoO7f3e9f875Doo is load14 0xOoOoO7f3e9f875Doo - 0xOoOoO7f3e9f876Doo is load15 0xOoOoO7f3e9f876Doo - 0xOoOoO7f3e9f878Doo is load16 0xOoOoO7f3e9f8aaDoo - 0xOoOoO7f3e9f8abDoo is load17 $\mathbf{X}\mathbf{X}\mathbf{X}\mathbf{X}\mathbf{X}\mathbf{X}\mathbf{X}\mathbf{X}\mathbf{X}\mathbf{X}\mathbf{X}\mathbf{X}\mathbf{X}\mathbf{X}\mathbf{X}\mathbf{X}\mathbf{X}\mathbf{X}\mathbf{X}\mathbf{X}\mathbf{X}$ 1 $\mathrm{x} = \mathrm{X} = \mathrm{X} = \mathrm{X} = \mathrm{X} = \mathrm{X} = \mathrm{X} = \mathrm{X} = \mathrm{X} = \mathrm{X} = \mathrm{X} = \mathrm{X} = \mathrm{X} = \mathrm{X} = \mathrm{X} = \mathrm{X} = \mathrm{X} = \mathrm{X} =$

```txt
0x000055e16505e1b0 - 0x000055e1650678d8 is .dynstr
0x000055e1650678d8 - 0x000055e165068bd8 is .gpu-version
0x000055e165068bd8 - 0x000055e165068ca8 is .gpu_version_r
0x000055e165068ca8 - 0x000055e165076928 is .rela.dyn
0x000055e165076928 - 0x000055e165077d98 is .rela.plt
0x000055e165078000 - 0x000055e165078017 is .init
0x000055e16507820 - 0x000055e165078dd0 is .plt
0x000055e165078ddo - 0x000055e165078de8 is .plt.got
0x000055e165078dfo - 0x000055e165125781 is .text
0x000055e165125784 - 0x000055e16512578d is .fini
0x000055e16512600 - 0x000055e16513f930 is .rodata
0x000055e16513f930 - 0x000055e165143df4 is .eh_frame hdr
0x000055e165143df8 - 0x000055e16515b730 is .eh_frame
0x000055e16515d3fO - 0x000055e16515d3f8 is .init_array
0x000055e16515d3f8 - 0x000055e16515d4Oo is .fini_array
\( \mathrm{X} \)  \( \mathrm{X} \)  \( \mathrm{X} \)  \( \mathrm{X} \) \( \mathrm{X} \) \( \mathrm{X} \) \( \mathrm{X} \) \( \mathrm{X} \) \( \mathrm{X} \) \( \mathrm{X} \) \( \mathrm{X} \) \( \mathrm{X} \) \( \mathrm{X} \) \( \mathrm{X} \) \( \mathrm{X} \) \( \mathrm{X} \) \( \mathrm{x} \) \( \mathrm{x} \) \( \mathrm{x} \) \( \mathrm{x} \) \( \mathrm{x} \) \( \mathrm{x} \) \( \mathrm{x} \) \( \mathrm{x} \) \( \mathrm{x} \) \( \mathrm{x} \) \( \mathrm{x} \) \( \mathrm{x} \) \( \mathrm{x} \) \( \mathrm{x} \) \( \mathrm{x}^{\prime } \) \( \mathrm{x}^{\prime } \) \( \mathrm{x}^{\prime } \) \( \mathrm{x}^{\prime } \) \( \mathrm{x}^{\prime } \) \( \mathrm{x}^{\prime } \) \( \mathrm{x}^{\prime } \) \( \mathrm{x}^{\prime } \) \( \mathrm{x}^{\prime } \) \( \mathrm{x}^{\prime } : \mathrm{x}^{\prime } \) \( \mathrm{x}^{\prime } : \mathrm{x}^{\prime } : \mathrm{x}^{\prime } : \mathrm{x}^{\prime } : \mathrm{x}^{\prime } : \mathrm{x}^{\prime } : \mathrm{x}^{\prime } : \mathrm{x}^{\prime } : \mathrm{x}^{\prime } : \mathrm{x}^{\prime } : \mathrm{x}^{\prime } : \mathrm{x}^{\prime } : : : : : : : : : : : : : : : : : : : : : : : : : : : : : : : : : : : : : : : : : : : : : : : : : : : : : : : : : : : : : : : : : : : : : : : : : : : : : : : : : :
\)
--Type <RET> for more, q to quit, c to continue without paging--
\( ①\quad ①\quad ①\quad ①\quad ①\quad ①\quad ①\quad ①\quad ①\quad ①\quad ①\quad ①\quad ①\quad ①\quad ①\quad ①\quad ①\quad ①\quad ①\quad ①\quad ①\left[ { / }\right] = { / }\left[ { / }\right] = { / }\left[ { / }\right] = { / }\left[ { / }\right] = { / }\left[ { / }\right] = { / }\left[ { / }\right] = { / }\left[ { / }\right] = { / }\left[ { / }\right] = { / }\left[ { / }\right] = { / }\left[ { / }\right] = { / }\lbrack   + { / }\rbrack   + { / }\rbrack   + { / }\rbrack   + { / }\rbrack   + { / }\rbrack   + { / }\rbrack   + { / }\rbrack   + { / }\rbrack   + { / }\rbrack   + { / }\rbrack   + { / }\rbrack   + { / }\rbrack   + { / }\rbrack   = { / }\left[ { / }\right] = { / }\left[ { / }\right] = { / }\left[ { / }\right] = { / }\left[ { / }\right] = { / }\left[ { / }\right] = { / }\left[ { / }\right] = { / }\left[ { / }\right] = { / }\left[ { / }\right] = { / }\left[ { / }\right] 
\)
	\( ②\quad ②\quad ②\quad ②\quad ②\quad ②\quad ②\quad ②\quad ②\quad ②\quad ②\quad ②\quad ②\quad ②\quad ②\quad ②\quad ②\quad ②\quad ②\quad ②\quad ②\left. {\begin{array}{l}{}}\\ {{/}\left| {\begin{array}{l}{}}\\ {{/}\left| {\begin{array}{l}{}}\\ {{/}\left| {\begin{array}{l}{}}\\ {{/}\left| {\begin{array}{l}{}}\\ {{/}\left| {\begin{array}{l}{}}\\ {{/}\left| {\begin{array}{l}{}}\\ {{/}\left| {\begin{array}{l}{}}\\ {{/}\left| {\boldsymbol {\mathbf{c}}}\right| {\boldsymbol {\mathbf{c}}}}\end{array}\right| {\boldsymbol {\mathbf{c}}}}\end{array}\right| {\boldsymbol {\mathbf{c}}}}\end{array}\right| {\boldsymbol {\mathbf{c}}}}\end{array}\right| }}\\ {{/}\left| {\begin{array}{l}{}}\\ {{/}\left| {\begin{array}{l}{}}\\ {{/}\left| {\boldsymbol {\mathbf{c}}}\right| {\boldsymbol {\mathbf{c}}}}\end{array}\right| {\boldsymbol {\mathbf{c}}}}\end{array}\right| }}\\ {{/}\left| {\begin{array}{l}{}}\\ {{/}\left| {\boldsymbol {\mathbf{c}}}\end{array}\right| {\boldsymbol {\mathbf{c}}}}\end{array}\right| }}\\ {{/}\left| {\begin{array}{l}{}}\\ {{/}\left| {\boldsymbol {\mathbf{c}}}\end{array}\right| {\boldsymbol {\mathbf{c}}}}\end{array}\right| }}\\ {{/}\left| {\begin{array}{l}{}}\\ {{/}\left| {\mathbf{c}}\end{array}\right| {\mathbf{c}}^{*}}} 
```

9. Load a core dump core.19649 and bash executable from the A64 directory:

```txt
\~/LAPI/x64\$ cd ../A64 
```

~/LAPI/A64$ gdb-multiarch -c core.19649 -se bash

GNU gdb (Debian $8 . 2 . 1 \AA - 2 + 6 3$ ) 8.2.1

Copyright (C) 2018 Free Software Foundation, Inc.

```txt
License GPLv3+: GNU GPL version 3 or later <http://gnu.org/licenses/gpl.html>  
This is free software: you are free to change and redistribute it.  
There is NO WARRANTY, to the extent permitted by law.  
Type "show copying" and "show warranty" for details.  
This GDB was configured as "x86_64-linux-gnu".  
Type "show configuration" for configuration details.  
For bug reporting instructions, please see:  
<http://www.gnu.org/software/gdb/bugs/>.  
Find the GDB manual and other documentation resources online at:  
<http://www.gnu.org/software/gdb/documentation/>. 
```

```txt
For help, type "help". Type "apropos word" to search for commands related to "word"... Reading symbols from bash...(no debugging symbols found)...done. 
```

```yaml
warning: core file may not match specified executable file. [New LWP 19649] 
```

```txt
warning: Could not load shared library symbols for 3 libraries, e.g. /lib/aarch64-linux-gnu/libtinfo.so.6. Use the "info sharedlibrary" command to see the complete listing. Do you need "set solib-search-path" or "set sysroot"? Core was generated by `-bash'. #0 0x0000fffffbafa6734 in ?? () 
```

```txt
(gdb) set solid-search-path . Reading symbols from /home/coredump/LAPI/A64/libtinfo.so.6...(no debugging symbols found)...done. Reading symbols from /home/coredump/LAPI/A64/libc.so.6...(no debugging symbols found)...done. Reading symbols from /home/coredump/LAPI/A64/ld-linux-aarch64.so.1...(no debugging symbols found)...done. 
```

10. Set logging to a file in case of lengthy output from some commands:

```txt
(gdb) set logging on L1.log Copying output to L1.log. 
```

11. Disassemble the shell_execve function and find the call to the open@plt function:

```asm
(gdb) disassemble shell_execute
Dump of assembler code for function shell_execute:
0x0000aaaabb6f7144 <=0>: stp x29, x30, [sp, #-224]!
0x0000aaaabb6f7148 <=4>: adr p x3, 0xaaabb800000
0x0000aaaabb6f714c <=8>: mov x29, sp
0x0000aaaabb6f7150 <=12>:ldr x3, [x3, #1192]
0x0000aaaabb6f7154 <=16>: stp x19, x20, [sp, #16]
0x0000aaaabb6f7158 <=20>: mov x19, x0
0x0000aaaabb6f715c <=24>: stp x21, x22, [sp, #32]
0x0000aaaabb6f7160 <=28>: mov x22, x1
0x0000aaaabb6f7164 <=32>: stp x23, x24, [sp, #48]
0x0000aaaabb6f7168 <=36>: mov x23, x2
0x0000aaaabb6f716c <=40>: stp x25, x26, [sp, #64]
0x0000aaaabb6f7170 <=44>:ldr x4, [x3]
0x0000aaaabb6f7174 <=48>: str x4, [sp, #216]
0x0000aaaabb6f7178 <=52>: mov x4, #0x0 // #0
0x0000aaaabb6f717c <=56>: bl 0xaaabb6d23e0 <execve@plt>
0x0000aaaabb6f7180 <=60>: bl 0xaaabb6d2740 <__errno_location@plt>
0x0000aaaabb6f7184 <=64>: mov x20, x0
0x0000aaaabb6f7188 <=68>: adr p x3, 0xaaabb80000
0x0000aaaabb6f718c <=72>:ldr x0, [x3, #2616]
0x0000aaaabb6f7190 <=76>:ldr w1, [x0]
0x0000aaaabb6f7194 <=80>: cbnz w1, 0xaaabb6f72bc <shell_execve+376> 
```

```asm
0x0000aaaabb6f7198: ++84>: ldr w21, [x20]  
0x0000aaaabb6f719c: ++88>: cmp w21, #0x8  
0x0000aaaabb6f71a0: ++92>: b.eq 0xaaabb6f73c4 <shell_execute+640> // b none  
0x0000aaaabb6f71a4: ++96>: adrp x23, 0xaaabb800000  
0x0000aaaabb6f71a8: ++100>: cmp w21, #0x2  
0x0000aaaabb6f71ac: ++104>: cset w2, eq // eq = none  
0x0000aaaabb6f71b0: ++108>: add x24, sp, #0x58  
0x0000aaaabb6f71b4: ++112>:ldr x3, [x23, #3744]  
0x0000aaaabb6f71b8: ++116>: add w2, w2, #0x7e  
0x0000aaaabb6f71bc: ++120>: mov x1, x24  
0x0000aaaabb6f71c0: ++124>: mov x0, x19  
0x0000aaaabb6f71c4: ++128>: str w2, [x3]  
0x0000aaaabb6f71c8: ++132>: bl 0xaaabb6d2180 <stat@plt>  
0x0000aaaabb6f71cc: ++136>: cbnz w0, 0xaaabb6f71e0 <shell_execute+156>  
0x0000aaaabb6f71d0: ++140>: ldr w0, [sp, #104]  
0x0000aaaabb6f71d4: ++144>: and w0, w0, #0xf000  
0x0000aaaabb6f71d8: ++148>: cmp w0, #0x4, ls1 #12  
0x0000aaaabb6f71dc: ++152>: b.eq 0xaaabb6f7288 <shell_execute+324> // b none  
0x0000aaaabb6f71e0: ++156>: mov x0, x19  
0x0000aaaabb6f71e4: ++160>: bl 0xaaabb742530 <file_status>  
0x0000aaaabb6f71e8: ++164>: tbnz w0, #4, 0xaaabb6f727c <shell_execute+312>  
0x0000aaaabb6f71ec: ++168>: mov w1, #0x12 // #18  
0x0000aaaabb6f71f0: ++172>: and w22, w0, w1  
--Type <RET> for more, q to quit, c to continue without paging--  
0x0xxxxxaaabbb6f71f4: ++176>: cmp w22, #0x2  
0xxxxxaaabbb6f71f8: ++18O>: b.ne 0xaaabb6f721c <shell_execute+216> // b.any  
0xxxxxaaabbb6f71fc: ++184>: cmp w21, #Ox7  
0xxxxxaaabbb6f722O: ++188>: ccmp w21, #Oxc, #Ox4, ne // ne = any  
0xxxxxaaabbb6f722O: ++192>: b.eq 0xaaabb6f721c <shell_execute+216> // b none  
0xxxxxaaabbb6f722O: ++196>: mov xO, x19  
0xxxxxaaabbb6f722Oc: ++2o>: mov w1, #OxO // #O  
Oxxxxxaaabbb6f721O: ++2o4>: bI  Oxaabbb6d1ecO <open@plt>  
Oxxxxxaaabbb6f7214: ++2o8>: mov w25, wO  
Oxxxxxaaabbb6f7218: ++2i2>: tbz wO, #3I,  Oxaabbb6f72dO <shell_execute+396>  
Oxxxxxaaabbb6f721c: ++2i6>: str w2I, [x2O]  
Oxxxxxaaabbb6f722O: ++2i2>: mov wO, w2I  
Oxxxxxaaabbb6f7224: ++2i4>: bl  Oxaabbb6d21IO <strerror@plt>  
Oxxxxxaaabbb6f7228: ++2i8>: mov x2, xO  
Oxxxxxaaabbb6f722c: ++2i3>: mov xI, xI9  
Oxxxxxaaabbb6f723D: ++i3>: adrp xO,  Oxaabbb7c4Oo  
Oxxxxxaaabbb6f7234: ++i4>: add xO, xO, #Oxbd8  
Oxxxxxaaabbb6f723B: ++i4>: bl  Oxaabbb7O6674 <report_error>  
Oxxxxxaaabbb6f723C: ++i4>: ldr x23, [x23, #3744]  
Oxxxxxaaabbb6f724O: ++i5>: ldr w25, [x23]  
Oxxxxxaaabbb6f7244: ++i5>: adrp xO,  Oxaabbb8OooOo  
Oxxxxxaaabbb6f7248: ++i5>: ldr xO, [xO, #I192]  
Oxxxxxaaabbb6f724c: ++i4>: ldr xZ, [sp, #I2I6]  
Oxxxxxaaabbb6f725O: ++i4>: ldr xI, [xO]  
Oxxxxxaaabbb6f7254: ++i7>: subs xZ, xZ, xI  
Oxxxxxaaabbb6f7258: ++i7>: mov xI, #Oxo// #O  
Oxxxxxaaabbb6f725c: ++i8>: b.ne  Oxaabbb6f77ec <shell_execute+17o4> // b.any  
Oxxxxxaaabbb6f726O: ++i8>: mov wO, wZ5  
Oxxxxxaaabbb6f7264: ++i8>: ldp xI9, xZo, [sp, #I6]  
Oxxxxxaaabbb6f7268: ++i9> : ldp xZI, xZo, [sp, #I3]  
Oxxxxxaaabbb6f726c: ++i9> : ldp xZ3, xZo, [sp, #I48]  
Oxxxxxaaabbb6f727O: ++i9> : ldp xZ5, xZo, [sp, #I64]  
Oxxxxxaaabbb6f7274: ++i9> : ldp xZ9, x3o, [sp], #I2I4  
Oxxxxxaaabbb6f7278: ++i9> : ret  
Oxxxxaaaabb6f727c: ++i3> : mov wI, #oXl5 // #I 2I  
Oxxxxaaaabb6f728O: +3i > str wI, [Xo] 
```

```txt
0x0000aaaabb6f7284 <+320>: b 0xaaabb6f71ec <shell_execve+168>  
0x0000aaaabb6f7288 <+324>: adr p x1, 0xaaabb7c4000  
0x0000aaaabb6f728c <+328>: add x1, x1, #0xbd8  
0x0000aaaabb6f7290 <+332>: mov w2, #0x5 // #5  
0x0000aaaabb6f7294 <+336>: mov x0, #0x0 // #0  
0x0000aaaabb6f7298 <+340>: bl 0xaaabb6d2630 <dcgett@plt>  
0x0000aaaabb6f729c <+344>: mov x20, x0  
0x0000aaaabb6f72a0 <+348>: mov w0, #0x15 // #21  
0x0000aaaabb6f72a4 <+352>: bl 0xaaabb6d2110 <strerror@plt>  
--Type <RET> for more, q to quit, c to continue without paging--q Quit 
```

12. Follow the call path to see the real branch and link to the libc.so.6 library:

```txt
(gdb) disassemble 0xaaabb6d1ec0  
Dump of assembler code for function open@plt:  
0x0000aaaabb6d1ec0 <= agrp x16, 0xaaabb7ff000  
0x0000aaaabb6d1ec4 <= ldr x17, [x16, #1464]  
0x0000aaaabb6d1ec8 <= add x16, x16, #0x5b8  
0x0000aaaabb6d1ecc <= br x17  
End of assembler dump. 
```

```txt
(gdb) x/a 0xffffabbb7ff000+1464  
0xffffabbb7ff5b8 <open@got.plt>: 0xfffffbafc7810 <open64> 
```

```txt
(gdb) info sharedlibrary  
From To Syms Read Shared Object Library  
0x0000ffffbb0ad860 0x0000ffffbb0cb88 Yes (*) /home/coredump/LAPI/A64/libtinfo.so.6  
0x0000ffffbaf17040 0x0000ffffbb023f20 Yes (*) /home/coredump/LAPI/A64/libc.so.6  
0x0000ffffbb0eec40 0x0000ffffbb10d064 Yes (*) /home/coredump/LAPI/A64/ld-linux-aarch64.so.1  
(*): Shared library is missing debugging information. 
```

13. We can also check the section for the address 0xaaaabb7ff000+1464 (0xaaaabb7ff5b8):

```txt
(gdb) info file   
Symbols from "/home/coredump/LAPI/A64/bash".   
Local core dump file: /home/coredump/LAPI/A64/core.19649', file type elf64-littlearch64. 0x0000aaaabb6a0000 - 0x0000aaaabb7ed000 is load1 0x0000aaaabb7fc000 - 0x0000aaaabb801000 is load2 0x0000aaaabb801000 - 0x0000aaaabb80a000 is load3 0x0000aaaabb80a000 - 0x0000aaaabb815000 is load4 0x0000aaaae4664000 - 0x0000aaaae4815000 is load5 0x000ffbaefo000 - 0x000ffbbb079000 is load6 0x000ffbbb088000 - 0x000ffbbb8c000 is load7 0x000ffbbb8c000 - 0x000ffbbb8e000 is load8 0x000ffbbb8e000 - 0x000ffbbb9a000 is load9 0x000ffbbbOaOoo - 0xOooffbbbOccOoo is load1O 0xOooffbbdbOdbOoo - 0xOooffbbdbfOoo is load11 0xOooffbbdbfOoo - 0xOooffbbdbeOoo is load12 0xOooffbbedOoo - 0xOooffbbbb118Ooo is load13 0xOooffbbb119Ooo - 0xOooffbbbb11bOoo is load14 0xOooo ffbbb122Ooo - 0xOooo ffbbb124Ooo is load15 0xOooo ffbbb126Ooo - 0xOooo ffbbb127Ooo is load16 0xOooo ffbbb127Ooo - 0xOooo ffbbb129Ooo is load17 0xOooo ffbbb129Ooo - 0xOooo ffbbb12bOoo is load18 0xOooo fffffc5fb8Ooo - 0xOooo fffffc5fd9Ooo is load19   
Local exec file: 
```

`/home/coredump/LAPI/A64/bash', file type elf64-littleaarch64.  
Entry point: 0xaaaabb6d4440  
0x0000aaaabb6a0238 - 0x0000aaaabb6a0253 is .interp  
0x0000aaaabb6a0254 - 0x0000aaaabb6a0278 is .note.gnu.build-id  
0x0000aaaabb6a0278 - 0x0000aaaabb6a0298 is .note.ABI-tag  
0x0000aaaabb6a0298 - 0x0000aaaabb6a4e54 is .gpuhash  
0x0000aaaabb6a4e58 - 0x0000aaaabb6b3a50 is .dynsym  
0x0000aaaabb6b3a50 - 0x0000aaaabb6bd6f7 is .dynstr  
0x0000aaaabb6bd6f8 - 0x0000aaaabb6beaa2 is .gpu(version)  
0x0000aaaabb6beaa8 - 0x0000aaaabb6beb38 is .gpu-version_r  
0x0000aaaabb6beb38 - 0x0000aaaabb6d0538 is .rela.dyn  
0x0000aaaabb6d0538 - 0x0000aaaabb6d1a50 is .rela.plt  
0x0000aaaabb6d1a50 - 0x0000aaaabb6d1a68 is .init  
0x0000aaaabb6d1a70 - 0x0000aaaabb6d28a0 is .plt  
0x0000aaaabb6d28c0 - 0x0000aaaabb7af8b0 is .text  
0x0000aaaabb7af8b0 - 0x0000aaaabb7af8c4 is .fini  
0x0000aaaabb7af8c8 - 0x0000aaaabb7c8f24 is .rodata  
0x0000aaaabb7c8f24 - 0x0000aaaabb7cd4f0 is .eh_frame hdr  
0x0000aaaabb7cd4f0 - 0x0000aaaabb7ec48o is .eh_frame  
OxOoOoAAAABB7fc818 -  OXoOoOAAAABB7fc82O is .init_array  
OxOoOoAAAABB7fc82O -  OXoOoOAAAABB7fc828 is .fini_array  
OxOoOoAAAABB7fc828 -  OXoOoOAAAABB7ff178 is .data.rel.ro  
OxOoOoAAAABB7ff178 -  OXoOoOAAAABB7ff388 is .dynamic  
OxOoOoAAAABB7ff388 -  OXoOoOAAAABB8O1Ooo is .got  
OxOoOoAAAABB8O1Ooo -  OXoOoOAAAABB8O928O is .data  
--Type $<\mathrm{RET}>$ for more, q to quit, c to continue without paging--q Quit

Note: We see that for ARM64 implementation it is .GOT (Global Offset Table) section.

Goal: Explore import/export information and calls to shared libraries.

ADDR Patterns: Call Path.

1. Launch WinDbg and load a core dump core.19649 from the A64 directory:

```txt
Microsoft (R) Windows Debugger Version 10.0.25324.1001 AMD64 Copyright (c) Microsoft Corporation. All rights reserved. 
```

```txt
Loading Dump File [C:\LAPI\A64\core.19649] 64-bit machine not using 64-bit API 
```

```txt
********** Path validation summary ****  
Response Time (ms) Location  
Deferred svr*  
Symbol search path is: svr*  
Executable search path is:  
Generic Unix Version 0 UP Free ARM 64-bit (AArch64)  
System Uptime: not available  
Process Uptime: not available  
*** WARNING: Unable to verify timestamp for libc.so.6  
*** WARNING: Unable to verify timestamp for bash  
libc.so+0xb6734:  
0000ffff`bafa6734 d4000001svc #0 
```

2. Set the symbol path and logging to a file in case of lengthy output from some commands:

```txt
0:000> .sympath+ C:\LAPI\A64   
Symbol search path is: srv\;C:\LAPI\A64   
Expanded Symbol search path is: cache\*;SRV\*\*https://msdl.microsoft.com/download/symbols;c:\\lapi\a64   
********** Path validation summary****************** Response Time (ms) Location Deferred srv\* OK C:\LAPI\A64   
*** WARNING: Unable to verify timestamp for libc.so.6   
*** WARNING: Unable to verify timestamp for bash 
```

```txt
0:000> .reload   
....\*\*\*WARNING: Unable to verify timestamp for libc.so.6   
\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\* 
```

```txt
********** Symbol Loading Error Summary ***  
Module name Error  
bash The system cannot find the file specified  
libc.so The system cannot find the file specified 
```

```txt
You can troubleshoot most symbol related issues by turning on symbol loading diagnostics (!sym noisy) and repeating the command that caused symbols to be loaded.  
You should also verify that your symbol search path (.sympath) is correct. 
```

0:000> .logopen C:\LAPI\A64\L1-WinDbg.log

Opened log file 'C:\LAPI\A64\L1-WinDbg.log'

3. Disassemble the shell_execve function and find the call to the address 0xaaaabb6d1ec0 identified in GDB exercises as open@plt function:

0:000> uf shell_execve   
bash!shell_execute:  
0000aaa`bb6f7144 a9b27bfd stp fp,lr,[sp,#-0xE0]!  
0000aaa`bb6f7148 b0000843 adr p,fp,sp  
0000aaa`bb6f714c 910003fd mov fp,sp  
0000aaa`bb6f7150 f9425463 ldr x3,[x3,#0x4A8]  
0000aaa`bb6f7154 a90153f3 stp x19,x20,[sp,#0x10]  
0000aaa`bb6f7158 aa0003f3 mov x19,x0  
0000aaa`bb6f715c a9025bf5 stp x21,x22,[sp,#0x20]  
0000aaa`bb6f7160 aa0103f6 mov x22,x1  
0000aaa`bb6f7164 a90363f7 stp x23,x24,[sp,#0x30]  
0000aaa`bb6f7168 aa0203f7 mov x23,x2  
0000aaa`bb6f716c a9046bf9 str p x25,x26,[sp,#0x40]  
0000aaa`bb6f7174 f9006fe4 str x4,[sp,#0xD8]  
0000aaa`bb6f7178 d280004 mov x4,#0  
0000aaa`bb6f717c 97ff6c99 b1 bash+o3x323e0 (0000aaa`bb6d23e0)  
0000aaa`bb6f7180 97ff6d70 b1 bash+o3x32740 (0000aaa`bb6d2740)  
0000aaa`bb6f7184 aa0003f4 mov x20,x0  
0000aaa`bb6f7188 b0000843 adr p x3,bash!o_options+o2x2118 (0000aaa`bb80000)  
0000aaa`bb6f718c f9451c6d ldr x3,[x3,#oxA38]  
0000aaa`bb6f719b b940001 ldr w1,[xO]  
0000aaa`bb6f7194 3500941 cbnz w1,bash!shell_execute+oX178 (              Branch  
bash!shell_execute+oX54:  
\( \begin{array}{l} \text{OOOOAAA} \text{BB} \text{B} \text{F} \text{7} \text{I} \text{9} \text{B} \text{B} \text{B} \text{B} \text{B} \text{B} \text{B} \text{B} \text{B} \text{B} \text{B} \text{B} \text{B} \text{B} \text{B} \text{B} \text{B} \text{B} \text{B} \text{B} \text{C} \text{D} \text{C} \text{D} \text{C} \text{D} \text{C} \text{D} \text{C} \text{D} \text{C} \text{D} \text{C} \text{D} \text{C} \text{D} \text{C} \text{D} \text{C} \text{D} \text{C} \text{C} \text{D} \text{C} \text{D} \text{C} \text{D} \text{C} \text{D} \text{C} \text{D} \text{C} \text{D} \text{C} \text{D} \text{C} \text{D} \text{C} \text{D} \text{C} \text{O}\right) \\ \mathrm{\Delta}\mathrm{\Delta}\mathrm{\Delta}\mathrm{\Delta}\mathrm{\Delta}\mathrm{\Delta}\mathrm{\Delta}\mathrm{\Delta}\mathrm{\Delta}\mathrm{\Delta}\mathrm{\Delta}\mathrm{\Delta}\mathrm{\Delta}\mathrm{\Delta}\mathrm{\Delta}\mathrm{\Delta}\mathrm{\Delta}\mathrm{\Delta}\mathrm{\Delta}\mathrm{\Delta}\mathrm{\Delta}\mathrm{\Delta}\mathrm{\Delta}\mathrm{\Delta}\mathrm{\Delta}\mathrm{\Delta}}\\ \mathrm{\Delta}\mathrm{\Delta}\mathrm{\Delta}\mathrm{\Delta}\mathrm{\Delta}\mathrm{\Delta}\mathrm{\Delta}\mathrm{\Delta}\mathrm{\Delta}\mathrm{\Delta}\mathrm{\Delta}\mathrm{\Delta}\mathrm{\Delta}\mathrm{\Delta}\mathrm{\Delta}\mathrm{\Delta}\mathrm{\Delta}\mathrm{\Delta}\mathrm{\Delta}\mathrm{\Delta}\mathrm{\Delta}\mathrm{\Delta}\mathrm{\Delta}\mathrm{\Delta}\mathrm{\Lambda}\\ \mathrm{\Delta}\mathrm{\Delta}\mathrm{\Delta}\mathrm{\Delta}\mathrm{\Delta}\mathrm{\Delta}\mathrm{\Delta}\mathrm{\Delta}\mathrm{\Delta}\mathrm{\Delta}\mathrm{\Delta}\mathrm{\Delta}\mathrm{\Delta}\mathrm{\Delta}\mathrm{\Delta}\mathrm{\Delta}\mathrm{\Delta}\mathrm{\Delta}\mathrm{\Delta}\mathrm{\Delta}\mathrm{\Delta}\mathrm{\Delta}\mathrm{\Delta}\mathrm{\Delta}\mathrm{\delta}\\ \mathrm{\Delta}\mathrm{\Delta}\mathrm{\Delta}\mathrm{\Delta}\mathrm{\Delta}\mathrm{\Delta}\mathrm{\Delta}\mathrm{\Delta}\mathrm{\Delta}\mathrm{\Delta}\mathrm{\Delta}\mathrm{\Delta}\mathrm{\Delta}\mathrm{\Delta}\mathrm{\Delta}\mathrm{\Delta}\mathrm{\Delta}\mathrm{\Delta}\mathrm{\Delta}\mathrm{\Delta}\mathrm{\Delta}\mathrm{\Delta}\mathrm{\Delta}\mathrm{\Delta}\mathrm{{\delta}}\\ \begin{array}{lclclclclclclclclclclclclclclclclclclclclclclclclclclclclclclclclclclclclclclclclclclclclclclclclclclclclclclclclclclclclclclclclclclclclclclclclclclclclclclclclclclclclclclclclclclclclclclCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLL CLL CLL CLL CLL CLL CLL CLL CLL CLL CLL CLL CLL CLL CLL CLL CLL CLL CLL CLL CLL CLL CLL CLL CLL CLL CLL CLL CLL CLL CLL CLL CLL CLL CLL CLL CLL CLL CLL CLL CLL CLL CLL CLL CLL CLL CLL CLL CLL CLL CLL C L L C L C L C L C L C L C L C L C L C L C L C L C L C L C L C L C L C L C L C L C L C L C L C L C L C L C L C L C L C L C L C L C L C L C L C L C L C L C L C L C L C L C L C L C L C L C L C L C L C L C L G D E G D E G D E G D E G D E G D E G D E G D E G D E G D E G D E G D E G D E G D E G D E G D E G D E G D E G D E G D E G D E G D E G D E G D E G D E G D E G D E G D E G D E G D E G D E G D E G D E G D E O O O O O A A A A A A A A A A A A A A A A A A A A A A A A A A A A A A A A A A A A A A A A A A A A A A A A A A A A A A A A A A A A A A A A A A A A A A A A A A A A A A A A A A A A A A A A A A A A A A A A A A A A B B B B B B B B B B B B B B B B B B B B B B B B B B B B B B B B B B B B B B B B B B B B B B B B B B B B B B B B B B B B B B B B B B B B B B B B B B B B B B B B B B B B B B B B B B B B B B B B B B B B BB BB BB BB BB BB BB BB BB BB BB BB BB BB BB BB BB BB BB BB BB BB BB BB BB BB BB BB BB BB BB BB BB BB BB BB BB BB BB BB BB BB BB BB BB BB BB BB BB BB BB BB BB BB BB BB BB BB BB BB BB BB BB BB BB BB BB BB BB BB BB BB BB BB BB BB BB BB BB BB BB BB BB BB BB BB BB BB BB BB BB BB BB BB BB BB BB BB BB BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBA

```csv
0000aaaab6f7210 97ff6b2c b1 bash+0x31ec0 (0000aaaab6d1ec0)   
0000aaaab6f7214 2a0003f9 mov w25,w0   
0000aaaab6f7218 36f805c0 tbz x0,#0x1F,bash!shell_execve+0x18c (0000aaaab6f72d0) Branch   
bash!shell_execve+0xd8:   
0000aaaab6f721c b9000295 str w21,[x20]   
0000aaaab6f7220 2a1503e0 mov w0,w21   
0000aaaab6f7224 97ff6bbb b1 bash+0x32110 (0000aaaab6d2110)   
0000aaaab6f7228 aa0003e2 mov x2,xo   
0000aaaab6f722c aa1303e1 mov x1,x19   
0000aaaab6f7230 b000066o adr p x0,bash!build_version+Ox1aa4 (Ooooooaaa'bb7c4Ooo)   
0000aaaab6f7234 912f6Ooo add xo,xo,#OxD8   
0000aaaab6f7238 94Ooo3dof b1 bash!report_error (Ooooooaaa'bb7Ooo674)   
bash!shell_execve+Oxf8:   
0000aaaab6f723c f94752f7 ldr x23,[x23,#OxAo]   
0000aaaab6f724D b94Ooo2f9 ldr w25,[x23]   
bash!shell_execve+Ox1Oo:   
0000aaaab6f7244 bOooOO84o adr p xO,bash!o_options+Ox2118 (Ooooooaaa'bb8OooOO)   
0000aaaab6f7248 f94254Oo ldr xo,[xo,#OxA48]   
0oOoooaaaab6f724c f94Ooofe2 ldr x2,[sp,#OxD8]   
0oOoooaaaab6f725D f94Ooooo1 ldr x1,[xO]   
OoOoooaaaab6f7254 ebol1Oo42 subs x2,x2,x1   
OoOoooaaaab6f7258 d28Ooooo1 mov x1,#o   
OoOoooaaaab6f725c 54Ooo2c81 bne bash!shell_execve+Ox6a8 (Ooooooaaa'bb6f77ec) Branch   
bash!shell_execve+Ox11c:   
OoOoooaaaab6f726D 2a19O3eO mov wO,w25   
OoOoooaaaab6f7264 a94153f3 ldp x19,x2o,[sp,#Ox1o]   
OoOoooaaaab6f7268 a9425bf5 ldp x21,x2,[sp,#Ox2o]   
OoOoooaaaab6f726c a94363f7 ldp x23,x24,[sp,#Ox3o]   
OoOoooaaaab6f727D a9446bf9 ldp x25,x26,[sp,#Ox4o]   
OoOoooaaaab6f7274 a8ce7bfd ldp fp,lr,[sp],#OxEo   
OoOoooaaaab6f7278 d65fO3c0 ret   
bash!shell_execve+Ox138:   
OoOoooaaaab6f727C 528Ooo2a1 mov w1,#Ox15   
OOoooaaaab6f728D b9OOOO2B1 str w1,[x2o]   
OOoooaaaab6f7284 17ffffda b bash!shell_execve+Oxa8 (OOoooaaa'bb6f71ec) Branch   
bash!shell_execve+Ox144:   
OOoooaaaab6f728B bOooOO661 adr p x1,bash!build_version+Ox1aa4 (OOoooaaa'bb7c4Ooo)   
OOoooaaaab6f728C 912f6Oo1 add x1,x, #OxCD8   
OOoooaaaab6f729D 528OOooa2 mov w2,#5   
OOoooaaaab6f7294 d 28OOOOov mov xo,#o   
OOoooaaaab6f7298 97ff6ce6 b1 bash+Ox3263D (OOoooaaa'bb6d263D)   
OOoooaaaab6f729C aaOOoo3f4 mov x2o,xo   
OOoooaaaab6f72ao 528OOooaov mov wO,#Ox15   
OOoooaaaab6f72A 97ff6b9b b1 bash+Ox3211D (OOoooaaa'bb6d211D)   
OOoooaaaab6f72a8 aa13Oo3e1 mov x1,x19   
OOoooaaaab6f72ac aaOOoo3e2 mov x2,xo   
OOoooaaaab6f72b  aa14Ooo3ov mov xo,xo   
OOoooaaaab6f72b4 94OOoo3df b1 bash!internal_error (OOoooaaa'bb7o5a3D)   
OOoooaaaab6f72B 17ffffeI b bash!shell_execve+Oxf8 (OOoooaaa'bb6f723c) Branch   
bash!shell_execve+Ox178:   
OOoooaaaab6f72bc d OooOO881 adr p x1,bash!rl_color_indicator+Ox128 (OOoooaaa'bb8O9OO)   
OOoooaaaab6f72COb 94QOOOO 1dr w,O,[xo]   
OOoooaaaab6f72C4 b948lOo1 ldr w,I,[x1,#oX8IO]   
OOoooaaaab6f72C8 35fff6B1 cbnz wI,bash!shell_execve+Ox54 (OOoooaaa'bb6f7198) Branch   
bash!shell_execve+Ox188:   
OOoooaaaab6f72cc 94QOOd4d I bI bash!sigint_sighandler+Ox15D (OOoooaaa'bb7c61D)   
bash!shell_execve+Ox18c:   
OOoooaaaab6f72dD aa18O3eI mov x1,x24   
OOoooaaaab6f72d4 d 28QIOIOU mov x2,#oX8O   
OOoooaaaab6f72d8 97ff6c96 bI bash+Ox3253D (OOoooaaa'bb6d253D)   
OOoooaaaab6f72dc aaOOoo3fa mov x26,xo   
OOoooaaaab6f72e O aa19QIOE o mov w,Ow,wo   
OOoooaaaab6f72e4 2aIAOTef mov wZsWz   
OOoooAAAab6f72e8 97ff6b92 bI bash+Ox3213D (OOoooAAA'bb6d213D)   
OOoooAAAab6f72Ec 7Ioooo3fc cmp wZs,#o   
OOoooAAAab6f72F O 54ff96d ble bash!shell_execve+Oxd8 (OOoooAAA'bb6f72ic) Branch 
```

```asm
bash!shell_execute+0x1b0:   
0000aaa`bb6f72f4 51000740 sub w0,w26,#1   
0000aaa`bb6f72f8 3820cb1f strb wzr,[x24,w0 sxtw #0]   
0000aaa`bb6f72fc 71000b5f cmp w26,#2   
0000aaa`bb6f7300 54fff8ed ble bash!shell_execute+0xd8 (0000aaa`bb6f721c) Branch   
bash!shell_execute+0xc0:   
0000aaa`bb6f7304 394163e0 ldrb w0,[sp,#0x58]   
0000aaa`bb6f7308 71008c1f cmp w0,#0x23   
0000aaa`bb6f730c 54fff881 bne bash!shell_execute+0xd8 (0000aaa`bb6f721c) Branch   
bash!shell_execute+0xc1cc:   
0000aaa`bb6f7310 394167e0 ldrb w0,[sp,#0x59]   
0000aaa`bb6f7314 7100841f cmp w0,#0x21   
0000aaa`bb6f7318 54fff821 bne bash!shell_execute+0xd8 (0000aaa`bb6f721c) Branch   
bash!shell_execute+0xd8:   
0000aaa`bb6f731c 91016be1 add x1,sp,#Ox5A   
bash!shell_execute+Ox1dc:   
0000aaa`bb6f7320 3940020 ldrb wO,[x1]   
0oOoAAA`bb6f7324 aaO1O3f7 mov x23,x1   
OoOoAAA`bb6f7328 71Oo8O1f cmp wO,#Ox2O   
OoOoAAA`bb6f732c 7a4918O4 ccmpne wO,#9,#4   
OoOoAAA`bb6f7330 54Oo1e81 bne bash!shell_execute+Ox5bc (OOOOAAA`bb6f77Oo) Branch   
bash!shell_execute+Ox1fO:   
OoOoAAA`bb6f7334 11Ooo6d6 add w22,w22,#1   
OoOoAAA`bb6f7338 91Ooo6e1 add x1,x23,#1   
OoOoAAA`bb6f733c 6b16O33f cmp w25,w22   
OoOoAAA`bb6f734D 54FFFo1 bne bash!shell_execute+Ox1dc (OOOOAAA`bb6f732O) Branch   
bash!shell_execute+Ox2Oo:   
OoOoAAA`bb6f7344 8b3ac317 add x23,x24,w26,sxtw #o   
OoOoAAA`bb6f7348 d28Ooo39 mov x25,#1   
OoOoAAA`bb6f734c d28Ooo1a mov x26,#o   
bash!shell_execute+Ox2C:   
OoOoAAA`bb6f7350 aa19O3eO mov xO,x25   
OoOoAAA`bb6f7354 97ff6ad3 bl bash+Ox31eaO (OOOOAAA`bb6d1eaO)   
OoOoAAA`bb6f7358 aaoooo3f8 mov x24,xo   
OOoAAA`bb6f735c b4oo272D cbz xO,bash!shell_execute+Ox6fc (OOOOAAA`bb6f784O) Branch   
bash!shell_execute+Ox2Ic:   
OoOoAAA`bb6f7360 aa17O3e1 mov x1,x23   
OOoAAA`bb6f7364 aaiaO3e2 mov x2,x26   
OOoAAA`bb6f7368 aa18O3eO mov xO,x24   
OOoAAA`bb6f736c 97ff69cdbl bash+Ox31aaO (OOOOAAA`bb6d1aaO)   
OOoAAA`bb6f7370 383a6b1f strb wzr,[x24,x26]   
OOoAAA`bb6f7374 aa18O3eO mov xO,x24   
OOoAAA`bb6f7378 97ff69e6 bl bash+Ox31b1O (OOOOAAA`bb6d1b1O)   
OOoAAA`bb6f737c b9oooo295 str w21,[x20]   
OOoAAA`bb6f738 934OTc1SsTw x2l,wO   
OOoAAA`bb6f7384 d1ooo6b6 sub x22,x21,#1   
OOoAAA`bb6f7388 38766b1l drb wI,[x24,x22]   
OOoAAA`bb6f738c 71oo343f cmp wI,#OxD   
OOoAAA`bb6f739D 54oo1eaobeq bash!shell_execute+Ox620 (OOOOAAA`bb6f7764) Branch   
bash!shell_execute+Ox250:   
OOoAAA`bb6f7394 528ooooa2 mov w2,#5   
OOoAAA`bb6f7398 dooooo641 adrp xI,bash!IO_std_used+Ox11738 (OOOOAAA`bb7c1oo)   
OOoAAA`bb6f739c d28oooo mov xO,##   
OOoAAA`bb6f73a0 91324Q21 add xI,xI,#OxC9O   
OOoAAA`bb6f73a4 97ff6ca3 bl bash+Ox3263 O (OOOOAA`bb6d263O)   
OOoAAA`bb6f73a8 528oofd9 mov w25,#Ox7E   
OOoAAA`bb6f73ac aaI3Obel mov xI,xI9   
OOoAAA`bb6f73b0 aaI8One2 mov xZ,xZ4   
OOoAAA`bb6f73b4 94ooBb44bl bash!sys_error (OOOOAAA~bb7ocC4)   
OOoAAA`bb6f73b8 aaI8Oneov m vX,OxZ4   
OOoAAA`bb6f73bc 97ff6bd9bl bash+Ox3232 O (OOOOAA~bb6d232O)   
OOoAAA`bb6f73c0 17ffffaI b bash!shell_execute+Ox1oo (OOOOAA~bb6f7244) Branch   
bash!shell_execute+Ox280:   
OOoAAA`bb6f73c4 aaIaONeov m x,Ox, 
```

<table><tr><td>0000aaa\bb6f73d0 2a0003f9 mov</td><td>w25,w0</td></tr><tr><td>0000aaa\bb6f73d4 36f81480 tbz</td><td>x0,#0x1F,bash!\shellisible+0x520 (0000aaa\bb6f7664) Branch</td></tr><tr><td>bash!\shellisible+0x294:</td><td></td></tr><tr><td>0000aaa\bb6f73d8 b0000854 adr</td><td>x20,bash!\o options+0x2118 (0000aaa\bb680000)</td></tr><tr><td>0000aaa\bb6f73dc 97ff77dd bl</td><td>bash!\reset_parser (0000aaa\bb6d5350)</td></tr><tr><td>0000aaa\bb6f73e0 f9421a80 ldr</td><td>x0,[x20,#0x430]</td></tr><tr><td>0000aaa\bb6f73e4 f9400000 ldr</td><td>x0,[x0]</td></tr><tr><td>0000aaa\bb6f73e8 b4000240 cbz</td><td>x0,bash!\shellisible+0x2ec (0000aaa\bb6f7430) Branch</td></tr><tr><td>bash!\shellisible+0x2a8:</td><td></td></tr><tr><td>0000aaa\bb6f73ec b9400c01 ldr</td><td>w1,[x0,#0xC]</td></tr><tr><td>0000aaa\bb6f73f0 34000081 cbz</td><td>w1,bash!\shellisible+0x2bc (0000aaa\bb6f7400) Branch</td></tr><tr><td>bash!\shellisible+0x2b0:</td><td></td></tr><tr><td>0000aaa\bb6f73f4 f0000181 adr</td><td>x1,bash!\shell_globfilename+0x10e0 (0000aaa\bb672a000)</td></tr><tr><td>0000aaa\bb6f73f8 9115c021 add</td><td>x1,x1,#0x570</td></tr><tr><td>0000aaa\bb6f73fc 9400a5d2 bl</td><td>bash!\phash Flush+0xb4 (0000aaa\bb6720b44)</td></tr><tr><td>bash!\shellisible+0x2bc:</td><td></td></tr><tr><td>0000aaa\bb6f7400 f9421a94 ldr</td><td>x20,[x20,#0x430]</td></tr><tr><td>0000aaa\bb6f7404 f9400295 ldr</td><td>x21,[x20]</td></tr><tr><td>0000aaa\bb6f7408 f94002a0 ldr</td><td>x0,[x21]</td></tr><tr><td>0000aaa\bb6f740c 97ff6bc5 bl</td><td>bash+0x32320 (0000aaa\bb6d2320)</td></tr><tr><td>0000aaa\bb6f7410 aa1503e0 mov</td><td>x0,x21</td></tr><tr><td>0000aaa\bb6f7414 97ff6bc3 bl</td><td>bash+0x32320 (0000aaa\bb6d2320)</td></tr><tr><td>0000aaa\bb6f7418 b0000840 adr</td><td>x0,bash!\o options+0x2118 (0000aaa\bb680000)</td></tr><tr><td>0000aaa\bb6f741c f900029f str</td><td>xzr,[x20]</td></tr><tr><td>0000aaa\bb6f7420 f942d400 ldr</td><td>x0,[x0,#0x5A8]</td></tr><tr><td>0000aaa\bb6f7424 b9400001 ldr</td><td>w1,[x0]</td></tr><tr><td>0000aaa\bb6f7428 321f0021 orr</td><td>w1,w1,#2</td></tr><tr><td>0000aaa\bb6f742c b9000001 str</td><td>w1,[x0]</td></tr><tr><td>bash!\shellisible+0x2ec:</td><td></td></tr><tr><td>0000aaa\bb6f7430 b0000840 adr</td><td>x0,bash!\o options+0x2118 (0000aaa\bb680000)</td></tr><tr><td>0000aaa\bb6f7434 f9407c00 ldr</td><td>x0,[x0,#0xF8]</td></tr><tr><td>0000aaa\bb6f7438 b900001f str</td><td>wzr,[x0]</td></tr><tr><td>0000aaa\bb6f743c 9400661d bl</td><td>bash!\without_job_control (0000aaa\bb6710cb0)</td></tr><tr><td>0000aaa\bb6f7440 52800220 mov</td><td>w0,#0x11</td></tr><tr><td>0000aaa\bb6f7444 f000041 adr</td><td>x1,bash!\initialize_shell_variables+0x26bc (0000aaa\bb6702000)</td></tr><tr><td>0000aaa\bb6f7448 91040021 add</td><td>x1,x1,#0x100</td></tr><tr><td>0000aaa\bb6f744c 9400ccfe bl</td><td>bash!\set_signal handler (0000aaa\bb672a844)</td></tr><tr><td>0000aaa\bb6f7450 f0000641 adr</td><td>x1,bash!\is basic_table+0x490 (0000aaa\bb67c2000)</td></tr><tr><td>0000aaa\bb6f7454 91144021 add</td><td>x1,x1,#0x510</td></tr><tr><td>0000aaa\bb6f7458 90000840 adr</td><td>x0,bash!\o options+0x118 (0000aaa\bb67ff000)</td></tr><tr><td>0000aaa\bb6f745c f9465c00 ldr</td><td>x0,[x0,#0XC88]</td></tr><tr><td>0000aaa\bb6f7460 ad400c22 ldp</td><td>q2,q3,[x1]</td></tr><tr><td>0000aaa\bb6f7464 ad410420 ldp</td><td>q0,q1,[x1,#0x20]</td></tr><tr><td>0000aaa\bb6f7468 f9402021 ldr</td><td>x1,[x1,#0x40]</td></tr><tr><td>0000aaa\bb6f746c ad000c02 stp</td><td>q2,q3,[x0]</td></tr><tr><td>0000aaa\bb6f7470 ad010400 stp</td><td>q0,q1,[x0,#0x20]</td></tr><tr><td>0000aaa\bb6f7474 f9002001 str</td><td>x1,[x0,#0x40]</td></tr><tr><td>0000aaa\bb6f7478 94002bc6 bl</td><td>bash!\reset_shell_flags (0000aaa\bb6702390)</td></tr><tr><td>0000aaa\bb6f747c b6000845 adr</td><td>x5,bash!\o options+0x2118 (0000aaa\bb680000)</td></tr><tr><td>0000aaa\bb6f7480 9000844 adr</td><td>x4,bash!\o options+0x1118 (0000aaa\bb67ff000)</td></tr><tr><td>0000aaa\bb6f7484 b6000843 adr</td><td>x3,bash!\o options+0x2118 (0000aaa\bb680000)</td></tr><tr><td>0000aaa\bb6f7488 b6000842 adr</td><td>x2,bash!\o options+0x2118 (0000aaa\bb680000)</td></tr><tr><td>0000aaa\bb6f748c b6000841 adr</td><td>x1,bash!\o options+0x2118 (0000aaa\bb680000)</td></tr><tr><td>0000aaa\bb6f7490 b6000840 adr</td><td>x0,bash!\o options+0x2118 (0000aaa\bb680000)</td></tr><tr><td>0000aaa\bb6f7494 f9456842 ldr</td><td>x2,[x2,#0xD0]</td></tr><tr><td>0000aaa\bb6f7498 5280026 mov</td><td>w6,#1</td></tr><tr><td>0000aaa\bb6f749c f943f421 ldr</td><td>x1,[x1,#0x7E8]</td></tr><tr><td>0000aaa\bb6f74a0 f9446800 ldr</td><td>x0,[x0,#0x8D0]</td></tr><tr><td>0000aaa\bb6f74a4 b900005f str</td><td>wzr,[x2]</td></tr><tr><td>0000aaa\bb6f74a8 f94164a5 ldr</td><td>x5,[x5,#0x2C8]</td></tr><tr><td>0000aaa\bb6f74ac b9000026 str</td><td>w6,[x1]</td></tr><tr><td>0000aaa\bb6f74b0 f9467484 ldr</td><td>x4,[x4,#0xC88]</td></tr><tr><td>0000aaa\bb6f74b4 b900006 str</td><td>w6,[x0]</td></tr><tr><td>0000aaa\bb6f74b8 f9441063 ldr</td><td>x3,[x3,#0x820]</td></tr><tr><td>0000aaa\bb6f74bc b90000bf str</td><td>wzr,[x5]</td></tr><tr><td>0000aaa\bb6f74c0 b900009f str</td><td>wzr,[x4]</td></tr><tr><td>0000aaa\bb6f74c4 b900007f str</td><td>wzr,[x3]</td></tr><tr><td>0000aaa\bb6f74c8 9401a546 bl</td><td>bash!\reset_shopt_options (0000aaa\bb67609e0)</td></tr><tr><td>0000aaa\bb6f74cc b6000840 adr</td><td>x0,bash!\o options+0x2118 (0000aaa\bb680000)</td></tr><tr><td>0000aaa\bb6f74d0 f9456c00 ldr</td><td>x0,[x0,#0xD8]</td></tr><tr><td>0000aaa\bb6f74d4 f9400001 ldr</td><td>x1,[x0]</td></tr></table>

0000aaa`bb6f74d8 b9400c22 ldr w2,[x1,#0xC]   
0000aaa`bb6f74dc 36180062 tbz x2,#3,bash!shell_execute+0x3a4 (0000aaa`bb6f74e8) Branch   
bash!shell_execute+0x39c:   
0000aaa`bb6f74e0 f9400c21 ldr x1,[x1,#0x18]   
0000aaa`bb6f74e4 f9000001 str x1,[x0]   
bash!shell_execute+0x3a4:   
0000aaa`bb6f74e8 f0000880 adr p x0,bash!subshell_top_level+0x50 (0000aaa`bb8o0a000)   
0000aaa`bb6f74ec f9415401 ldr x1,[x0,#0x2A8]   
0000aaa`bb6f74f0 b4000041 cbz x1,bash!shell_execute+0x3b4 (0000aaa`bb6f74f8) Branch   
bash!shell_execute+0x3b0:   
0000aaa`bb6f74f4 f901541f str xzr,[xO,#OxA8]   
bash!shell_execute+Ox3b4:   
0000aaa`bb6f74f8 b0000842 adr p x2,bash!o_options+Ox2118 (Oooaaa`bb8oOOOO)   
0000aaa`bb6f74fc b0ooo847 adr p x7,bash!o_options+Ox2118 (Oooaaa`bb8oOOOO)   
0oooaaa`bb6f75o 0b 0ooo846 adr p x6,bash!o_options+Ox2118 (Oooaaa`bb8oOOOO)   
0oooaaa`bb6f75o4 b 0ooo845 adr p x5,bash!o_options+Ox2118 (Oooaaa`bb8oOOOO)   
Oooaaa`bb6f75o8 f9431o42 ldr x2,[x2,#Ox62O]   
Oooaaa`bb6f75oc b 0ooo844 adr p x4,bash!o_options+Ox2118 (Oooaaa`bb8oOOOO)   
Oooaaa`bb6f751o b 0ooo843 adr p x3,bash!o_options+Ox2118 (Oooaaa`bb8oOOOO)   
Oooaaa`bb6f7514 b 0ooo841 adr p x1,bash!o_options+Ox2118 (Oooaaa`bb8oOOOO)   
Oooaaa`bb6f7518 b 0ooo84O adr p xO,bash!o_options+Ox2118 (OoooAAA`bb8oOOOO)   
Oooaaa`bb6f751c f945doe7 ldr x7,[x7,#OxABO]   
Oooaaa`bb6f752o f94948c6 ldr x6,[x6,#Ox9O]   
Oooaaa`bb6f7524 f946bfo5 ldr x5,[x5,#OxD6O]   
Oooaaa`bb6f7528 b9ooooff str wzr,[x7]   
Oooaaa`bb6f752c f9423484 ldr x4,[x4,#Ox468]   
Oooaaa`bb6f753o b9oooof str wzr,[x6]   
Oooaaa`bb6f7534 f945D063 ldr x3,[x3,#OxAOO]   
Oooaaa`bb6f7538 b9ooobf str wzr,[x5]   
Oooaaa`bb6f753c f94la421 ldr x1,[x1,#Ox348]   
Oooaaa`bb6f754o b9ooo9f str wzr,[x4]   
Oooaaa`bb6f7544 f946a8o 1dr xO,[xO,#OxD5O]   
Oooaaa`bb6f7548 b9oooof str wzr,[x3]   
Oooaaa`bb6f754c b94DQ 1dr w2,[x2]   
Oooaaa`bb6f755o b9DQ 1dr wzr,[x1]   
Oooaaa`bb6f7554 b9DQ 1dr wzr,[xO]   
Oooaaa`bb6f7558 34DQ 1d2 cbz w2,bash!shell_execute+Ox6bc (OoooAAA`bb6f78OO) Branch   
bash!shell_execute+Ox418:   
Ooooaaa`bb6f75cc 94DBeOEbl bash!set_sigint_handler (OoooAAA`bb726d8O)   
OoooAAA`bb6f756o aa16o3eov mov xO,x22   
OoooAAA`bb6f7564 94Dlda13 bl bash!strvec_len (OoooAAA`bb76ddbO)   
OoooAAA`bb6f7568 2aDoo3f5 mov w2i,wO   
OoooAAA`bb6f756c aa16o3eov mov xO,x22   
OoooAAA`bb6f757o 11Dooaa1 add w1,w21,#2   
OoooAAA`bb6f7574 94Dlq937bl bash!strvec_RESize (OoooAAA`bb76da5O)   
OoooAAA`bb6f7578 934DteB6 sxtw x22,w21   
OoooAAA`bb6f757c aaDoo3f mov x2D, $x_0$ OoooAAA`bb6f758o 91Doo6d6 add x22,x22,#1   
OoooAAA`bb6f758d d37dEea2 ubfiz x2,x21,#3,#Ox20   
OoooAAA`bb6f7588 cb3542cQ sub xO,x22,w21,uxtw # O   
OoooAAA`bb6f758c 11Dooob5 add w2i,w21,#1   
OoooAAA`bb6f759o d3dFto 1sI xO,x,O#3   
OoooAAA`bb6f759d d1Doo2D 1sub xI,x,O#8   
OoooAAA`bb6f759g 8bDQ 2B add xO,x2D, x  
OoooAAA`bb6f759c 8bDQ 1add xI,xDQ, xI   
OoooAAA`bb6f75ao 97ff6948bl bash+OtX3ac( OoooAAA`bb6d1acO)   
OoooAAA`bb6f75a4 b 0DIOBBG 1 adrp xO,bash!o_options+OtX2118 ( OoooAAA ` bb8DQOOOO)   
OoooAAA`bb6f75a8 f9413cQ ldr xO,[xO,#OtX27R]   
OoooAAA`bb6f75ac f94DQOOl dr xO,[xO]   
OoooAAA`bb6f75b0 a9DQeEO stp xO,x19,[x20]   
OoooAAA`bb6f75b4 f8367aQf str xZr,[xZo,xZ2 Isl #3]   
OoooAAA`bb6f75b8 f94DQ 2B ldr xO,[xZo]   
OoooAAA`bb6f75bc 394DQ 1drib wI,[xO]   
OoooAAA`bb6f75c0 71Dbo43f cmp wI,#OtXD Branch   
OoooAAA`bb6f75c4 54DQQ 1bne bash!shell_execute+OtX48c ( OoooAAA`bb6f75d) Branch

bash!shell_execve+0x48c:

```csv
0000aaaab6f75d0 9000840 adr x0,bash!o_options+0x1118 (0000aaaabb7ff000)   
0000aaaab6f75d4 f947e000 ldr x0,[x0,#0xCC0]   
0000aaaab6f75d8 b940000ldr w0,[x0]   
0000aaaab6f75dc 350010a0 cbnz w0,bash!shell_execute+0x6ac (0000aaaabbb6f77f0) Branch   
bash!shell_execute+0x49c:   
0000aaaab6f75e0 b0000856 adr x22,bash!o_options+0x2118 (0000aaaabbb80000)   
0000aaaab6f75e4 b0000853 adr x19,bash!o_options+0x2118 (0000aaaabbb8000)   
0000aaaab6f75e8 f94102d9 ldr x25,[x22,#0x200]   
0000aaaab6f75ec f94032o ldr x0,[x25]   
0000aaaab6f75fob40018cbz xO,bash!shell_execute+Ox4dc (Oooaaaaa'bb6f762O) Branch   
bash!shell_execute+Ox4bO:   
Oooaaaab6f75f4 f946567a ldr x26,[x19,#OxA8]   
Oooaaaab6f75f8 d28Ooo18 mov x24,#O   
Oooaaaab6f75fc 14OOOOO3 b bash!shell_execute+Ox4c4 (Oooaaaabbb6f76O8) Branch   
bash!shell_execute+Ox4bc:   
Oooaaaab6f76Oo f87878Oo ldr xO,[xO,x24 1s1 #3]   
Oooaaaab6f76O4 97ff6b47 bl bash+Ox3232O (OOOOAAA'bb6d232O)   
bash!shell_execute+Ox4c4:   
Oooaaaab6f76Oo b94Ooo341 ldr w1,[x26]   
Oooaaaab6f76Oc 91ooo718 add x24,x24,#1   
Oooaaaab6f761o f94Ooo32o ldr xO,[x25]   
Oooaaaab6f7614 6b18Ooo3cmp w1,w24   
Oooaaaab6f7618 54fff4c bgt bash!shell_execute+Ox4bc (OOOOAAA'bb6f76Oo) Branch   
bash!shell_execute+Ox4d8:   
Oooaaaab6f761c 97ff6b41 bl bash+Ox3232O (OOOOAAA'bb6d232O)   
bash!shell_execute+Ox4dc:   
Oooaaaab6f762o f9ooo898 adr x24,bash!subshell_top_level+Ox5O (OOOOAAA'bb8oAoo)   
Oooaaaab6f7624 f9413fOo ldr xO,[x24,#Ox278]   
Oooaaaab6f7628 97ffc472 b1 bash!dispose_command (OOOOAAA'bb6e87fO)   
Oooaaaab6f762c f9o13f1 str xZr,[x24,#Ox278]   
Oooaaaab6f763o 9oooo84o adr xO,bash!o_options+Ox1118 (OOOOAAA'bb7ffOO)   
Oooaaaab6f7634 f9465673 ldr x19,[x19,#OxA8]   
Oooaaaab6f7638 f9462ooo ldr xO,[xO,#OxC4O]   
Oooaaaab6f763c f941o2d6 ldr x22,[x22,#Ox2o]   
Oooaaaab6f764o b9ooo275 str w21,[x19]   
Oooaaaab6f7644 f9oooio17 str x23,[xO]   
Oooaaaab6f7648 f9ooo2d4 str x2O,[x22]   
Oooaaaab6f764c 97ff7a5d bl bash!unbind_args (OOOOAAA'bb6d5fc)   
Oooaaaab6f765o 94ooo84o bl bash!clear_fifo_list (OOOOAAA'bb7175)   
OOOOAAAabb6f765 9oooo84o adr xO,bash!o_options+Ox1118 (OOOOAAA'bb7ffOO)   
OOOOAAAabb6f7658 528ooo21 mov w1,#1   
OOOOAAAabb6f765c f94ec0o ldr xO,[xO,#OxFD8]   
OOOOAAAabb6f766o 97ff6b78 bl bash+Ox3244D (OOOOAAA'bb6d244D)   
bash!shell_execute+Ox52o:   
Oooaaaabb6f7664 91o163f8 add x24,sp,#Ox58   
Oooaaaabb6f768 d28o1oou mov x2,#Ox8O   
Oooaaaabb6f76cc aa18o3e1 mov x1,x24   
Oooaaaabb6f767o 97ff6bbo bl bash+Ox3253 (OOOOAAA'bb6d253)   
OOoooabb6f767a aaooof5 mov x2I,xo   
OOoooabb6f7678 2a19o3eov mov w,Ow, w25   
OOoooabb6f767c 2a15o3f9 mov w25,w21   
OOoooabb6f768o 97ff6aoc b1 bash+Ox3213D (OOOOAAA'bb6d213D)   
OOoooabb. \(^{\mathrm{b}}\mathrm{b}^{*}\mathrm{S}\mathrm{F}^{*}\mathrm{S}\mathrm{B}^{*}\mathrm{S}\mathrm{C}^{*}\mathrm{B}\mathrm{F}^{*}\mathrm{C}^{*}\mathrm{B}\mathrm{F}^{*}\mathrm{C}^{*}\mathrm{B}\mathrm{F}^{*}\mathrm{C}^{*}\mathrm{B}\mathrm{F}^{*}\mathrm{C}^{*}\mathrm{B}\mathrm{F}^{*}\mathrm{C}^{*}\mathrm{B}\mathrm{F}^{*}\mathrm{C}^{*}\mathrm{\Lambda}\mathrm{B}\mathrm{F}^{*}\mathrm{\Lambda}\mathrm{B}\mathrm{F}^{*}\mathrm{\Lambda}\mathrm{B}\mathrm{F}^{*}\mathrm{\Lambda}\mathrm{B}\mathrm{F}^{*}\mathrm{\Lambda}\mathrm{B}\mathrm{F}^{*}\mathrm{\Lambda}\mathrm{B}\mathrm{F}^{*}\mathrm{\Lambda}\mathrm{B}\mathrm{F}^{*}\mathrm{\Lambda}\mathrm{\Lambda}\mathrm{\Lambda}\mathrm{\Lambda}\mathrm{\Lambda}\mathrm{\Lambda}\mathrm{\Lambda}\mathrm{\Lambda}\mathrm{\Lambda}\mathrm{\Lambda}\mathrm{\Lambda}\mathrm{\Lambda}\mathrm{\Lambda}\mathrm{\Lambda}\mathrm{\Lambda}\mathrm{\Lambda}\mathrm{\Lambda}\mathrm{\Lambda}\mathrm{\Lambda}\mathrm{\Lambda}\mathrm{\Lambda}\mathrm{\Lambda}\mathrm{\Lambda}\mathrm{\Lambda}\mathrm{\Lambda}\mathrm{\AA} 
```

```csv
bash!shell_execute+0x568:  
0000aaa`bb6f76ac 8b030300 add x0,x24,x3  
0000aaa`bb6f76b0 385ff000 ldurb w0,[x0,#-1]  
0000aaa`bb6f76b4 7100281f cmp w0,#0xA Branch  
0000aaa`bb6f76b8 54ffe900 beq bash!shell_execute+0x294 (0000aaa`bb6f73d8) Branch  
bash!shell_execute+0x578:  
0000aaa`bb6f76bc 35ffff20 cbnz w0,bash!shell_execute+0x55c (0000aaa`bb6f76a0) Branch  
bash!shell_execute+0x57c:  
0000aaa`bb6f76c0 d0000641 adr p x1,bash!IO_std_used+0x11738 (0000aaa`bb6c1000)  
0000aaa`bb6f76c4 9132a021 add x1,x1,#0xC8  
0000aaa`bb6f76c8 52800a2 mov w2,#5  
0000aaa`bb6f76cc d280000 mov x0,#0  
0000aaa`bb6f76d0 97ff6bd8 b1 bash+0x32630 (0000aaa`bb6d2630)  
0000aaa`bb6f76d4 aa0003f5 mov x21,xo  
0000aaa`bb6f76d8 52801oo mov w#,#8  
0000aaa`bb6f76dc 97ff6a8d b1 bash+Ox3211O (OoOoAAA`bb6d211O)  
0000aaa`bb6f76eO aa13O3e1 mov x1,x19  
0000aaa`bb6f76e4 aaOoO3e2 mov x2,xo  
0000aaa`bb6f76e8 aa15O3eO mov xo,x21  
0oOoAAA`bb6f76ec 94Oo38d1 b1 bash!internal_error (OooAAA`bb7O5a3O)  
OOoAAA`bb6f76fO 528Oo1oo mov wO,#8  
OOoAAA`bb6f76f4 528OoFd9 mov w25,#Ox7E  
OOoAAA`bb6f76f8 b9Ooo28O str wO,[x2O]  
OOoAAA`bb6f76fc 17ffed2 b bash!shell_execute+Ox1oO (OooOAAA`bb6f7244) Branch  
bash!shell_execute+Ox5bc:  
OOoAAA`bb6f77O O 6b16O35f cmp w26,w22  
OOoAAA`bb6f77O4 54Ooo98d ble bash!shell_execute+Ox6fO (OooOAAA`bb6f7834) Branch  
bash!shell_execute+Ox5c4:  
OOoAAA`bb6f77O8 11Ooo6c3 add w3,w22,#1  
OOoAAA`bb6f77C 2a16o3e2 mov w2,w22  
OOoAAA`bb6f77I O 8b23c3O add x3,x24,w3,sxtw #o  
OOoAAA`bb6f77I4 14OooOoo5 b bash!shell_execute+Ox5e4 (OOooAAA`bb6f7728) Branch  
bash!shell_execute+Ox5d4:  
OOoAAA`bb6f77I8 11Ooo442 add w2,w2,#1  
OOoAAA`bb6f77Ic 6bO2o33f cmp w25,w2  
OOoAAA`bb6f772O 54Ooo18O beq bash!shell_execute+Ox6C (OOooAAA`bb6f775O) Branch  
bash!shell_execute+Ox5eO:  
OOoAAA`bb6f7724 384o146D ldrb wO,[x3],#1  
bash!shell_execute+Ox5e4:  
OOoAAA`bb6f7728 51oo24o1 sub w1,Ow,#9  
OOoAAA`bb6f77C 71oo8OIf cmp wO,#Ox2o  
OOoAAA`bb6f773 O 12oo1c2o and wO,w1,#OxFF  
OOoAAA`bb6f7734 7a4i18C ocmpne wO,#1,#o  
OOoAAA`bb6f7738 54fffofBghi bash!shell_execute+Ox5d4 (OOooAAA`bb6f7718) Branch  
bash!shell_execute+Ox5f8:  
OOoAAA`bb6f773c 4b16o42 sub w2,w2, w22  
OOoAAA`bb6f774 O 11oo459 add w25,w2,#1  
OOoAAA`bb6f7744 934oTc5a sxtw x26,w2  
OOoAAA`bb6f7748 934oTc39 sxtw x25,w25  
OOoAAA`bb6f774c 17ffefOb bash!shell_execute+Ox2OC (OOooAAA`bb6f735O) Branch  
bash!shell_execute+Ox6C:  
OOoAAA`bb6f77C 4b16o35a sub w26,w2, w22  
OOoAAA`bb6f7754 11oo459 add w25,w2, #1  
OOoAAA`bb6f7758 934oTc5a sxtw x26,w2  
OOoAAA`bb6f77C 934oTc39 sxtw x25,w25  
OOoAAA`bb6f77C 17ffefcb bash!shell_execute+Ox2OC (OOooAAA`bb6f735O) Branch  
bash!shell_execute+Ox6Z:  
OOoAAA`bb6f77C 11oo8i4 add w2o,W,#2  
OOoAAA`bb6f77C aa18O3eO mov xo,x24  
OOoAAA`bb6f77C 934oTc9 sxtw x2o,W2O  
OOoAAA`bb6f77D aa14o3eI mov x1,x2o  
OOoAAA`bb6f77A 97ff6aFbl bash+Ox32OB (OOoooAA `bb6d2OB)  
OOoAAA`bb6f77C 91ooocA I add x1,x2,#1  
OOoAAA`bb6f77C aaooofB mov x24,x 
```

```csv
bash!shell_execute+0x640:   
0000aaa`bb6f7784 52800bc0 mov w0,#0x5E   
0000aaa`bb6f7788 38366b00 strb w0,[x24,x22]   
0000aaa`bb6f778c 528009a0 mov w0,#0x4D   
0000aaa`bb6f7790 38356b00 strb w0,[x24,x21]   
0000aaa`bb6f7794 38216b1f strb wzr,[x24,x1]   
0000aaa`bb6f7798 17fffeff b bash!shell_execute+0x250 (0000aaa`bb6f7394) Branch   
bash!shell_execute+0x658:   
0000aaa`bb6f779c aa1403e1 mov x1,x20   
0000aaa`bb6f77a0 90005c0 adr p x0,bash!rl_enable_paren_MATCHing+0x87d0 (0000aaa`bb7af000)   
0000aaa`bb6f77a4 91332000 add x0,x0,#oXCC8   
0000aaa`bb6f77a8 94012bca bl bash!setup_execqing+Ox1O (OOOOAAA`bb7426dO)   
0000aaa`bb6f77ac 52800bc1 mov w1,#oX5E   
0000aaa`bb6f77b0 528O9aO mov wO,#oX4D   
0000aaa`bb6f77b4 381ff2a1 sturb w1,[x21,#-1]   
0000aaa`bb6f77b8 528Ooo2 mov w2,#5   
0000aaa`bb6f77bc 39Ooo2aO strb wO,[x21]   
0000aaa`bb6f77c0 dOoooo641 adr p x1,bash!IO_std_used+Ox11738 (OOOOAAA`bb7c1Ooo)   
0oOoooaa`bb6f77c4 39Ooo6bf strb wzr,[x21,#1]   
OOOOAAA`bb6f77c8 91324O21 add x1,x1,#oXC9O   
OOOOAAA`bb6f77cc d28Ooooo mov xO,#O   
OOOOAAA`bb6f77dO 528Oof9 mov w25,#oX7E   
OOOOAAA`bb6f77d4 97ff6b97 bl bash+Ox3263O (OOOOAAA`bb6d263O)   
OOOOAAA`bb6f77d8 aa13O3e1 mov x1,x19   
OOOOAAA`bb6f77dc dOoooo642 adr p x2,bash!IO_std_used+Ox11738 (OOOOAAA`bb7c1Ooo)   
OOOOAAA`bb6f77eO 91236O42 add x2,x2,#oX8D8   
OOOOAAA`bb6f77e4 94Ooo3a3B bl bash!sys_error (OOOOAAA`bb7o6Oc4)   
OOOOAAA`bb6f77e8 17fffe97 b bash!shell_execute+Ox1Oo (OOOOAAA`bb6f7244) Branch   
bash!shell_execute+Ox6a8:   
OOOOAAA`bb6f77ec 97ff6a4d bl bash+Ox3212O (OOOOAAA`bb6d212O)   
bash!shell_execute+Ox6ac:   
OOOOAAA`bb6f77fO 528Ooo561 mov w1,#oX2B   
OOOOAAA`bb6f77f4 528Ooo4O mov wO,#oX72   
OOOOAAA`bb6f77f8 94Ooo4516 bl bash!change_flag (OOOOAAA`bb7o8c5O)   
OOOOAAA`bb6f77fc 17fffff79 b bash!shell_execute+Ox49c (OOOOAAA`bb6f75eO) Branch   
bash!shell_execute+Ox6bc:   
OOOOAAA`bb6f78O O 9ooooo854 adr p x2o,bash!o_options+Ox11I8 (OOOOAAA`bb7ffooo)   
OOOOAAA`bb6f78O4 f946ae94 ldr x2o,[x2o,#oXD58]   
OOOOAAA`bb6f78O8 b94oou28o ldr wO,[x2o]   
OOOOAAA`bb6f78Oc 71oooofcmp wO,#o   
OOOOAAA`bb6f781O 54ffea6d ble bash!shell_execute+Ox418 (OOOOAAA`bb6f755c) Branch   
bash!shell_execute+Ox6d:   
OOOOAAA`bb6f7814 94oob13 bl bash!close_buffered_fd (OOOOAAA`bb72246O)   
OOOOAAA`bb6f781B 8D DADP xo,bash!o_options+Ox21I8 (OOOOAAA`bb8Doooo)   
OOOOAAA`bb6f781C 128Doooo1 mov w1,#-1   
OOOOAAA`bb6f782B 9Doooo28l str m w1,[x2o]   
OOOOAAA`bb6f782F f945a4oD ldr xo,[xo,#oXB4B]   
OOOOAAA`bb6f782B 9Doooofostr wzr,[xo]   
OOOOAAA`bb6f782c b9Dooo1 OStr w1,[xo,#oX1O]   
OOOOAAA`bb6f783O 17ffff4b b bash!shell_execute+Ox418 (OOOOAAA`bb6f755c) Branch   
bash!shell_execute+Ox6f:   
OOOOAAA`bb6f7834 d28Ooo1a mov x26,#o   
OOOOAAA`bb6f783B d28Ooo39 mov x25,#1   
OOOOAAA`bb6f783C 17ffec5 b bash!shell_execute+Ox2OC (OOOOAAA`bb6f735O) Branch   
bash!shell_execute+Ox6fc:   
OOOOAAA`bb6f784o aa19O3e1 mov x1,x25   
OOOOAAA`bb6f7844 9Doooo5cdoadr xo,bash!rl_enable_paren_MATCHing+Ox8yD0 (DOODAAA `bb7afDoo)   
OOOGAAA`bb6f784B 91236Doo add xo,xo,#oX8D8   
DOODAAA `bb6f784C 94DIOBAbl bash!setup_execqing+Ox1O (DOODAAA `bb7426dO)   
DOODAAA `bb6f785O 17ffec4 b bash!shell_execute+Ox2Ic (DOODAAA `bb6f736O) Branch 
```

# 4. Follow the call path to see the real branch and link to the libc.so.6 library:

```csv
0:000> u 0xaaaabb6d1ec0  
bash+0x31ec0:  
0000aaaa\bb6d1ec0 d0000970 adr p xip0,bash!o_options+0x1118 (0000aaa\bb7ff000)  
0000aaaa\bb6d1ec4 f942de11 ldr xip1,[xip0,#0x5B8]  
0000aaaa\bb6d1ec8 9116e210 add xip0,xip0,#0x5B8  
0000aaaa\bb6d1ecc d61f0220 br xip1  
0000aaaa\bb6d1ed0 d0000970 adr p xip0,bash!o_options+0x1118 (0000aaa\bb7ff000)  
0000aaaa\bb6d1ed4 f942e211 ldr xip1,[xip0,#0x5C0]  
0000aaaa\bb6d1ed8 91170210 add xip0,xip0,#0x5C0  
0000aaaa\bb6d1edc d61f0220 br xip1 
```

0:000> dps 0000aaaa`bb7ff000+0x5B8   
```txt
0000aaa\bb7ff5b8 0000ffff\bafc7810 libc(so!_open64   
0000aaa\bb7ff5c0 0000ffff\bafe57c0 libc(so!_fdelt_chk   
0000aaa\bb7ff5c8 0000ffff\bafe39c0 libc.so!_strncpy_chk   
0000aaa\bb7ff5d0 0000ffff\baf98240 libc.so!tzset   
0000aaa\bb7ff5d8 0000ffff\bafe3860 libc.so!_strcpy_chk   
0000aaa\bb7ff5e0 0000ffff\baf92970 libc.so!wcswidth   
0000aaa\bb7ff5e8 0000ffff\bafa8040 libc.so!getppid   
0000aaa\bb7ff5f0 0000ffff\baf2b040 libc.so!sigemptyset   
0000aaa\bb7ff5f8 0000ffff\baf807e4 libc.so!strncmp   
0000aaa\bb7ff600 0000ffff\baf89ceo libc.so!wcsncmp   
0000aaa\bb7ff608 0000ffff\baf248aO libc.so!bindtextdomain   
0000aaa\bb7ff61O 0000ffff\baf2b36O libc.so!_libc_current_sigrtmin   
0000aaa\bb7ff618 0000ffff\baf7ffOo libc.so!strcat   
0000aaa\bb7ff62O 0000ffff\bafe3c1O libc.so!_printf_chk   
0000aaa\bb7ff628 0000ffff\baf62fcO libc.so!_fpurge   
0000aaa\bb7ff63O 0000ffff\baf88fcO libc.so!strerrordesc_np+Ox14aO 
```

# 5. We can also check the regions for loaded shared libraries:

0:000> !address   
```diff
+ 0'0000000 aaa'bb6a0000 aaa'bb6a0000 <unknown>  
+ aaa'bb6a0000 aaa'bb7ed000 0'0014d000 MEM_PRIVATE MEM_COMMIT PAGE_EXECUTE_READ Image [bash; "/usr/bin/bash"]  
+ aaa'bb7ed000 aaa'bb7fc000 0'000f000 Image [bash; "/usr/bin/bash"]  
+ aaa'bb7fc000 aaa'bb801000 0'0005000 MEM_PRIVATE MEM_COMMIT PAGE_READONLY Image [bash; "/usr/bin/bash"]  
+ aaa'bb801000 aaa'bb8a0000 0'0009000 MEM_PRIVATE MEM_COMMIT PAGE_READWRITE Image [bash; "/usr/bin/bash"]  
+ aaa'bb8a0000 aaa'bb815000 0'000b6000 MEM_PRIVATE MEM_COMMIT PAGE_READWRITE <unknown>  
+ aaa'bb815000 aaa' e4664000 0'284e4f00 <unknown>  
+ aaa'e464000 aaa' e4815000 0'01b1b000 MEM_PRIVATE MEM_COMMIT PAGE_READWRITE <unknown>  
+ aaa'e4815000 ffff'babb8000 5554'd639b000 <unknown>  
+ fffff'babb8b8b8b8b8b8b8b8b8b8b8b8b8b8b8b8b8b8b8b8b8b8b8b8b8b8b8b8b8b8b8b8b8b8b8b8b8b8b8b8b8b8b8b8b8b8b8b8b8b8b 
```

# VS Code and WSL

Get started   
Troubleshooting

Add to .bashrc

alias code="'/mnt/c/Users/[USER]/AppData/Local/Programs/Microsoft Vs Code/Code.exe' "

2023 Software Diagnostics Services

To browse Linux API headers, we recommend using some IDE, for example, Visual Studio Code. It works with WSL but may require some troubleshooting.

# Get started

https://learn.microsoft.com/en-us/windows/wsl/tutorials/wsl-vscode

# Calling Convention

ioctl (from documentation)   
Actual declaration (sys/ioctl.h)

extern int ioctl (int _fd， unsigned long int _request,..*) THROW   
THROW is defined in sys/cdefs .h   
Argument passing order (simplified)

x64 left-to-right via RDI，RSI，RDX，RCX，R8，R9，right-to-left PUSH   
A64 left-to-right via X0 - X7，[SP]，[SP+8]，[SP+16]

System V Application Binary Interface: AMD64 Architecture Processor Supplement

2023Software Diagnostics Services

Except for some API calls that do not need arguments, most API calls require them. How such a source code is implemented on a particular platform is a subject of the so-called calling convention. In the x64 calling convention, the first six parameters are passed via registers from left to right and the rest – via stack (usually implemented via push hence negative RSP addressing, it also requires the caller to clear the stack by adjusting RSP after the call). In the ARM64 calling convention, the first eight parameters are passed via registers from left to right and the rest – via the stack, too (usually implemented via direct stack addressing in the preallocated region, hence positive SP addressing). Both are illustrated on the next two slides, and both platforms are illustrated in Exercise L2. I chose the ioctl function as it allows passing many parameters. Usually, Linux API only requires a few parameters. If you are interested in more details, the x64 ABI link has more information about the calling convention used.

# ioctl

https://man7.org/linux/man-pages/man2/ioctl.2.html

System V Application Binary Interface: AMD64 Architecture Processor Supplement

https://github.com/hjl-tools/x86-psABI/wiki/x86-64-psABI-1.0.pdf

# Parameter Passing (x64)

Test8params(int p1，int p2，int p3，int p4，int p5，int p6，int p7，int p8)

# Caller

EDI (p1)

ESI (p2)

EDX (p3)

ESX (p4)

R8D (p5)

R9D (p6)

![](images/c24505820db421df9de4927c9d4ff2b0460c6cdbbac6579e8f3185dbb85c78cf.jpg)

RSP: 0 p7

RSP+0x8:0' p8

# Callee

EDI (p1)

ESI (p2)

EDX (p3)

ESX (p4)

R8D (p5)

R9D (p6)

RSP: return address

RSP+0x8: 0'p7

RSP+0x10: 0' p8

2023Software Diagnostics Services

# Parameter Passing (A64)

Test10params(int p1,int p2， int p3，int p4，int p5，int p6，int p7,int p8，int p9,int p10);

# Caller

WO （p1

W1 (p2)

W2 (p3)

W3 n4

W4 (p5)

W5 (p6)

W6 (p7)

W7 (p8)

![](images/4471c8d34a439c10928a1aed98fdc1f81f630d8941d772d8f70cef917a7c6ff0.jpg)

# Callee

W0 (p1)

W1 (p2)

W2 (p3)

W3 (p4)

W4 (p5)

W5 (p6)

W6 （p7

W7 (p8)

sp: 0 p9

SP+8: 0`p10

sp: 0 p9

SP+8: 0'p10

2023Software Diagnostics Services

# Exercise L2

Goal: Compare calling conventions on x64 and A64 platforms   
ADDR Patterns: Call Prologue; Call Parameter; Function Prologue   
\LAPI-Dumps\Exercise-L2-GDB.pdf   
\LAPI-Dumps\Exercise-L2-WinDbg.pdf

2023Software Diagnostics Services

Goal: Compare calling conventions on x64 and A64 platforms.

ADDR Patterns: Call Prologue; Call Parameter; Function Prologue.

1. Load a core dump core.8323 and ioctl-params executable from the x64 directory:

```txt
~/LAPI/x64$ gdb -c core.8323 -se ioct1-params
GNU gdb (Debian 8.2.1-2+b3) 8.2.1
Copyright (C) 2018 Free Software Foundation, Inc.
License GPLv3+: GNU GPL version 3 or later <http://gnu.org/licenses/gpl.html>
This is free software: you are free to change and redistribute it.
There is NO WARRANTY, to the extent permitted by law.
Type "show copying" and "show warranty" for details.
This GDB was configured as "x86_64-linux-gnu".
Type "show configuration" for configuration details.
For bug reporting instructions, please see:
<http://www.gnu.org/software/gdb/bugs/>. 
Find the GDB manual and other documentation resources online at:
<http://www.gnu.org/software/gdb/documentation/>. 
For help, type "help".
Type "apropos word" to search for commands related to "word"... 
Reading symbols from ioct1-params...(no debugging symbols found)...done.
[New LWP 8323]
Core was generated by `./ioct1-params'。
#0 0x00007f77d6368594 in __GI__nanosleep (requested_time=requested_time@entry=0x7fff5574a3c0,
remaining=remaining@entry=0x7fff5574a3c0)
at ../sysdeps/unix/sysv/linux/nanosleep.c:28
28 ..sysdeps/unix/sysv/linux/nanosleep.c: No such file or directory. 
```

2. Set logging to a file in case of lengthy output from some commands:

```txt
(gdb) set logging on L2.log Copying output to L2.log. 
```

3. Disassemble the main function and find the call to the ioctl@plt function:

```asm
(gdb) disassemble main
Dump of assembler code for function main:
0x00005648dd4d145 <=0>: push %rbp
0x00005648dd4d146 <=1>: mov %rsp,%rbp
0x00005648dd4d149 <=4>: sub $0x10,%rsp
0x00005648dd4d14d <=8>: mov %edi,-0x4(%rbp)
0x00005648dd4d150 <=11>: mov %rsi,-0x10(%rbp)
0x00005648dd4d154 <=15>: mov $0xFFFFFFF,%edi
0x00005648dd4d159 <=20>: callq 0x5648dd4d040 <sleep@plt>
0x00005648dd4d15e <=25>: sub $0x8,%rsp
0x00005648dd4d162 <=29>: pushq $0xa
0x00005648dd4d164 <=31>: pushq $0x9
0x00005648dd4d166 <=33>: pushq $0x8
0x00005648dd4d168 <=35>: pushq $0x7
0x00005648dd4d16a <=37>: pushq $0x6
0x00005648dd4d16c <=39>: mov $0x5,%r9d
0x00005648dd4d172 <=45>: mov $0x4,%r8d
0x00005648dd4d178 <=51>: mov $0x3,%ecx 
```

```txt
0x00005648dd44d17d <+56>: mov $0x2,%edx
0x00005648dd44d182 <+61>: mov $0x1,%esi
0x00005648dd44d187 <+66>: mov $0x0,%edi
0x00005648dd44d18c <+71>: mov $0x0,%eax
0x00005648dd44d191 <+76>: callq 0x5648dd44d030 <ioct1@plt>
0x00005648dd44d196 <+81>: add $0x30,%rsp
0x00005648dd44d19a <+85>: leaveq
0x00005648dd44d19b <+86>: retq
End of assembler dump. 
```

Note: We see the first 6 parameters are passed via registers, then the next 5 parameters are pushed to stack. The caller has to adjust the stack pointer (RSP) by 0x30 $( 6 ^ { * } 8 )$ instead of $( 5 ^ { * } 8 )$ because there was an additional sub 8 instruction.

4. Load a core dump core.45713 and ioctl-params executable from the A64 directory:

```txt
\~/LAPI/x64\$ cd ../A64   
\~/LAPI/A64\$ gdb-multiarch -c core.45713 -se ioctl-params   
GNU gdb (Debian 8.2.1-2+b3) 8.2.1   
Copyright (C) 2018 Free Software Foundation, Inc.   
License GPLv3+: GNU GPL version 3 or later <http://gnu.org/licenses/gpl.html>   
This is free software: you are free to change and redistribute it.   
There is NO WARRANTY, to the extent permitted by law.   
Type "show copying" and "show warranty" for details.   
This GDB was configured as "x86_64-linux-gnu".   
Type "show configuration" for configuration details.   
For bug reporting instructions, please see: <http://www.gnu.org/software/gdb/bugs/>.   
Find the GDB manual and other documentation resources online at: <http://www.gnu.org/software/gdb/documentation/>.   
For help, type "help".   
Type "apropos word" to search for commands related to "word"...   
Reading symbols from ioctl-params...(no debugging symbols found)...done. [New LWP 45713]   
warning: Could not load shared library symbols for 2 libraries, e.g. /lib/aarch64-linux-gnu/libc.so.6.   
Use the "info sharedlibrary" command to see the complete listing.   
Do you need "set solib-search-path" or "set sysroot"?   
Core was generated by ./ioctl-params'.   
#0 0x0000ffff832b189c in ?? ()   
(gdb) set solid-search-path .   
Reading symbols from /home/coredump/LAPI/A64/libc.so.6...(no debugging symbols found)...done. Reading symbols from /home/coredump/LAPI/A64/ld-linux-aarch64.so.1...(no debugging symbols found)...done. 
```

5. Set logging to a file in case of lengthy output from some commands:

```txt
(gdb) set logging on L2.log Copying output to L2.log. 
```

6. Disassemble the main function and find the branch and link to the ioctl@plt function:

```asm
(gdb) disassemble main
Dump of assembler code for function main:
0x0000aaaab63007d4 ++0>:
sub sp, sp, #0x40
0x0000aaaab63007d8 ++4>:
stp x29, x30, [sp, #32]
0x0000aaaab63007dc ++8>:
add x29, sp, #0x20
0x0000aaaab63007e0 ++12>:
str w0, [sp, #60]
0x0000aaaab63007e4 ++16>:
str x1, [sp, #48]
0x0000aaaab63007e8 ++20>:
mov w0, #0xFFFFFFF
0x0000aaaab63007ec ++24>:
bl 0xaaab6300650 <sleep@plt>
0x0000aaaab63007f0 ++28>:
mov w0, #0xa
0x0000aaaab63007f4 ++32>:
str w0, [sp, #16]
0x0000aaaab63007f8 ++36>:
mov w0, #0x9
0x0000aaaab63007fc ++40>:
str w0, [sp, #8]
0x0000aaaab6300800 ++44>:
mov w0, #0x8
0x0000aaaab6300804 ++48>:
str w0, [sp]
0x0000aaaab6300808 ++52>:
mov w7, #0x7
0x0000aaaab63008oC ++56>:
mov w6, #Ox6
0x0000aaaab630081O ++6O>:
mov w5, #Ox5
0x0000aaaab6300814 ++64>:
mov w4, #Ox4
0x0000aaaab6300818 ++68>:
mov w3, #Ox3
0x0000aaaab630081c ++72>:
mov w2, #Ox2
0x0000aaaab630082O ++76>:
mov x1, #Ox1
0x0000aaaab6300824 ++8O>:
mov wO, #OxO
0x0000aaaab630oB28 ++84>:
bl 0xaaab63Ooo68o <ioctl@plt>
0x0oOoaaaab63Ooo82c ++88>:
ldp x29, x3O, [sp, #32]
OxOoooaaaab63Ooo83O ++92>:
add sp, sp, #Ox4O
OxOoooaaaab63Ooo834 ++96>:
ret 
```

Note: We see the first 8 parameters are passed via registers, then the next 3 parameters are saved in the already preallocated stack, so the caller doesn’t have to adjust the stack pointer (SP) right after the call.

# Exercise L2 (WinDbg)

Goal: Compare calling conventions on x64 and A64 platforms.

ADDR Patterns: Call Prologue; Call Parameter; Function Prologue.

1. Launch WinDbg and load a core dump core.45713 from the A64 directory:

```txt
Loading Dump File [C:\LAPI\A64\core.45713]  
64-bit machine not using 64-bit API  
********** Path validation summary ****  
Response Time (ms) Location  
Deferred svr*  
Symbol search path is: svr*  
Executable search path is:  
Generic Unix Version 0 UP Free ARM 64-bit (AArch64)  
System Uptime: not available  
Process Uptime: not available  
*** WARNING: Unable to verify timestamp for libc.so.6  
libc.so+0xb189c:  
0000ffff832b189c d4000001svc #0 
```

2. Set the symbol path and logging to a file in case of lengthy output from some commands:

```txt
0:000> .sympath+ C:\LAPI\A64   
Symbol search path is: srv\;C:\LAPI\A64   
Expanded Symbol search path is: cache\*;SRV\*\*https://msdl.microsoft.com/download/symbols;c:\\lapi\a64   
********** Path validation summary****************** Response Time (ms) Location Deferred srv\ OK C:\LAPI\A64   
*** WARNING: Unable to verify timestamp for libc.so.6 
```

```txt
0:000> .reload   
...*** WARNING: Unable to verify timestamp for libc.so.6   
********** Symbol Loading Error Summary ***   
Module name Error   
libc.so The system cannot find the file specified 
```

You can troubleshoot most symbol related issues by turning on symbol loading diagnostics (!sym noisy) and repeating the command that caused symbols to be loaded. You should also verify that your symbol search path (.sympath) is correct.

```batch
0:000> .logopen C:\LAPI\A64\L2-WinDbg.log  
Opened log file 'C:\LAPI\A64\L2-WinDbg.log' 
```

3. Disassemble the main function and find the branch and link to the address 0xaaaab6300680, identified as ioctl@plt function in the GDB exercise:

```csv
0:000>uf main  
ioct1.params!main:  
0000aaaa`b63007d4 d10103ff sub sp,sp,#0x40  
0000aaaa`b63007d8 a9027bfd stp fp,lr,[sp,#0x20]  
0000aaaa`b63007dc 910083fd add fp,sp,#0x20  
0000aaaa`b63007e0 b9003feo str w0,[sp,#0x3C]  
0000aaaa`b63007e4 f9001be1 str x1,[sp,#0x30]  
0000aaaa`b63007e8 12800000 mov w0,#-1  
0000aaaa`b63007ec 97fffff99 bl ioct1.params+0x650 (0000aaaa`b6300650)  
0000aaaa`b63007f0 52800140 mov w0,#0xA  
0000aaaa`b63007f4 b90013eO str wO,[sp,#Ox1O]  
0000aaaa`b63007f8 5280012O mov wO,#9  
0000aaaa`b63007fc b90OObeO str wO,[sp,#8]  
0000aaaa`b63008O O 528Ooo1Oo mov wO,#8  
0000aaaa`b63Ooo4 b9Ooo3eO str wO,[sp]  
000Oaaa`b63Ooo8 528Oooe7 mov w7,#7  
O O O O O O O c 528OooC6 mov w6,#6  
O O O O O A 528Oooa5 mov w5,#5  
O O O O A A B 528Ooo84 mov w4,#4  
O O O O A A B 528Ooo63 mov w3,#3  
O O O O A A B 528Ooo42 mov w2,#2  
O O O O A A B 528Ooo21 mov x1,#1  
O O O O A A B 528OooOo mov wO,#O  
O O O O A A B 97fffff96bl ioctl.params+Ox68O (Ooooaaa`b63Ooo68O)  
O O O O A A B 63Ooo2c a9427bfd ldp fp,lr,[sp,#Ox2O]  
O O O O A A B 63Ooo3O 91O1O3ff add sp,sp,#Ox4O  
O O O O A A B 63Ooo34 d65fO3cO ret 
```

Note: We see the first 8 parameters are passed via registers, then the next 3 parameters are saved in the already preallocated stack, so the caller doesn’t have to adjust the stack pointer (SP) right after the call.

# Static Linkage

![](images/ae01331272f1135e9367646c90e4aadc904337d8c73647d01b5f3562b71e8959.jpg)

# Example

core.18256 from /LAPI/x64/static-example

```txt
main:  
...  
callq 0x43c1f0 <sleep>  
...  
callq 0x43d6e0 <ioct1>  
... 
```

2023Software Diagnostics Services

For comparison, I created a statically linked example to show that in such a case, Linux API is called directly.

# Shared Libraries

![](images/80a0bfb052ea1cd7a8bd1967a57ccfa232c22dcfc7178c5825b1a4eb300497b5.jpg)

# GDB Commands

(gdb) info sharedlibrary   
(gdb) info files   
(gdb) info functions pattern

# WinDbg Commands

0:000>1m   
0:000> X mpattern!pattern   
0:000>× *!pattern

2023Software Diagnostics Services

Shared libraries can import functions from other shared libraries. But function names are not unique. Several shared libraries may export the same function names. Such functions may or may not have the same parameters. So, function syntax, semantics, and pragmatics are subject to API design. Also, not all functions from shared libraries are imported ‒ only those that are actually used in code.

# Modules and Analysis Patterns

Module memory analysis patterns

Module Collection   
Coupled Modules   
Duplicated Module

Namespace malware analysis pattern

2023Software Diagnostics Services

There are many memory analysis patterns related to modules (executables and shared libraries). On this slide, I list a few of them related to module dependencies and imported functions. Module Collection lists loaded modules. Coupled Modules is about module dependencies. Sometimes, the same module name may be loaded twice, for example, from different locations, the so-called Duplicated Module. Imported functions constitute the so-called Namespace malware analysis pattern which can give hints at overall module functionality and purpose. For example, an innocuous module name may have functions imported from LIBC that reveal potential network connectivity.

Note: all referenced patterns from dumpanalysis.org (and hundreds of others) are also available in Memory Dump Analysis Anthology volumes or Encyclopedia of Crash Dump Analysis Patterns (see References slides).

# Exercise L3

Goal: Explore shared libraries and their dependencies   
Memory Analysis Patterns: Module Collection; Coupled Modules; Value References   
Malware Analysis Patterns: Namespace   
\LAPI-Dumps\Exercise-L3-GDB.pdf   
\LAPI-Dumps\Exercise-L3-WinDbg.pdf

2023Software Diagnostics Services

Goal: Explore shared libraries and their dependencies.

Memory Analysis Patterns: Module Collection; Coupled Modules.

Malware Analysis Patterns: Namespace.

1. Check library dependencies of bash using ldd:

```txt
~/LAPI/x64$ ldd -v bash
linux-vdso.so.1 (0x00007ffd5cd53000)
libtinfo.so.6 => /lib/x86_64-linux-gnu/libtinfo.so.6 (0x00007fb8f43a6000)
libdl.so.2 => /lib/x86_64-linux-gnu/libdl.so.2 (0x00007fb8f43a1000)
libc.so.6 => /lib/x86_64-linux-gnu/libc.so.6 (0x00007fb8f41e1000)
/lib64/ld-linux-x86-64.so.2 (0x00007fb8f4509000)
Version information:
./bash:
libdl.so.2 (GLIBC_2.2.5) => /lib/x86_64-linux-gnu/libdl.so.2
libtinfo.so.6 (NCURSES6_TINFO_5.0.19991023) => /lib/x86_64-linux-gnu/libtinfo.so.6
libc.so.6 (GLIBC_2.11) => /lib/x86_64-linux-gnu/libc.so.6
libc.so.6 (GLIBC_2.14) => /lib/x86_64-linux-gnu/libc.so.6
libc.so.6 (GLIBC_2.8) => /lib/x86_64-linux-gnu/libc.so.6
libc.so.6 (GLIBC_2.15) => /lib/x86_64-linux-gnu/libc.so.6
libc.so.6 (GLIBC_2.4) => /lib/x86_64-linux-gnu/libc.so.6
libc.so.6 (GLIBC_2.3.4) => /lib/x86_64-linux-gnu/libc.so.6
libc.so.6 (GLIBC_2.3) => /lib/x86_64-linux-gnu/libc.so.6
libc.so.6 (GLIBC_2.2.5) => /lib/x86_64-linux-gnu/libc.so.6
/lib/x86_64-linux-gnu/libtinfo.so.6:
libc.so.6 (GLIBC_2.3) => /lib/x86_64-linux-gnu/libc.so.6
libc.so.6 (GLIBC_2.14) => /lib/x86_64-linux-gnu/libc.so.6
libc.so.6 (GLIBC_2.16) => /lib/x86_64-linux-gnu/libc.so.6
libc.so.6 (GLIBC_2.4) => /lib/x86_64-linux-gnu/libc.so.6
libc.so.6 (GLIBC_2.3.4) => /lib/x86_64-linux-gnu/libc.so.6
libc.so.6 (GLIBC_2.2.5) => /lib/x86_64-linux-gnu/libc.so.6
/lib/x86_64-linux-gnu/libdl.so.2:
Id-linux-x86-64.so.2 (GLIBC_PRIVATE) => /lib64/ld-linux-x86-64.so.2
libc.so.6 (GLIBC_PRIVATE) => /lib/x86_64-linux-gnu/libc.so.6
libc.so.6 (GLIBC_2.4) => /lib/x86_64-linux-gnu/libc.so.6
libc.so.6 (GLIBC_2.2.5) => /lib/x86_64-linux-gnu/libc.so.6
/lib/x86_64-linux-gnu/libc.so.6:
Id-linux-x86-64.so.2 (GLIBC_2.3) => /lib64/ld-linux-x86-64.so.2
Id-linux-x86-64.so.2 (GLIBC_PRIVATE) => /lib64/ld-linux-x86-64.so.2 
```

2. Load a core dump core.9 and bash executable from the x64 directory:

```txt
~/LAPI/x64$ gdb -c core.9 -se bash
GNU gdb (Debian 8.2.1-2+b3) 8.2.1
Copyright (C) 2018 Free Software Foundation, Inc.
License GPLv3+: GNU GPL version 3 or later <http://gnu.org/licenses/gpl.html>
This is free software: you are free to change and redistribute it.
There is NO WARRANTY, to the extent permitted by law.
Type "show copying" and "show warranty" for details.
This GDB was configured as "x86_64-linux-gnu".
Type "show configuration" for configuration details.
For bug reporting instructions, please see:
<http://www.gnu.org/software/gdb/bugs/>. 
Find the GDB manual and other documentation resources online at:
<http://www.gnu.org/software/gdb/documentation/>. 
```

```txt
For help, type "help".  
Type "apropos word" to search for commands related to "word"...  
Reading symbols from bash...(no debugging symbols found)...done.  
warning: core file may not match specified executable file.  
[New LWP 9]  
Core was generated by `-bash`.  
#0 0x00007f3e9f7492d7 in __GI__waitpid (pid=-1, stat_location=0x7ffc661ad0, options=10) at ../sysdeps/linux/sysv/linux/waitpid.c:30  
30 ../sysdeps-linux/sysv/linux/waitpid.c: No such file or directory. 
```

3. Set logging to a file in case of lengthy output from some commands:

```txt
(gdb) set logging on L3.log Copying output to L3.log. 
```

4. Find the shared library the _dl_update_slotinfo function is from:

```txt
(gdb) info address _dl_update_slotinfo
Symbol "_dl_update_slotinfo" is a function at address 0x7f3e9f895100. 
```

```txt
(gdb) info sharedlibrary  
From To Syms Read Shared Object Library  
0x00007f3e9f856950 0x00007f3e9f863dc8 Yes (*) /lib/x86_64-linux-gnu/libtinfo.so.6  
0x00007f3e9f844130 0x00007f3e9f844eb5 Yes /lib/x86_64-linux-gnu/libdl.so.2  
0x00007f3e9f6a5320 0x00007f3e9f7eb14b Yes /lib/x86_64-linux-gnu/libc.so.6  
0x00007f3e9f884090 0x00007f3e9f8a1b50 Yes /lib64/ld-linux-x86-64.so.2  
0x00007f3e9f37e300 0x00007f3e9f384578 Yes /lib/x86_64-linux-gnu/libnss_files.so.2  
(*): Shared library is missing debugging information. 
```

5. Disassemble the _dl_update_slotinfo function and find the call to the free function:

```asm
(gdb) disassemble_d1_update_slotinfo
Dump of assembler code for function_d1_update_slotinfo:
0x00007f3e9f895100 <+0> : push %r15
0x00007f3e9f895102 <+2> : push %r14
0x00007f3e9f895104 <+4> : push %r13
0x00007f3e9f895106 <+6> : push %r12
0x00007f3e9f895108 <+8> : push %rbp
0x00007f3e9f895109 <+9> : push %rbx
0x00007f3e9f89510a <+10> : sub $0x38,%rsp
0x00007f3e9f89510e <+14> : mov %fs:0x8,%r10
0x00007f3e9f895117 <+23> : mov 0x16e8a(%rip),%r14 # 0x7f3e9f8abfa8
<_rtld_global+3912>
0x00007f3e9f89511e <+30> : mov %r10,%r15
0x00007f3e9f895121 <+33> : mov %rdi,%rdx
0x00007f3e9f895124 <+36> : mov (%r14),%rcx
0x00007f3e9f895127 <+39> : mov %r14,%rsi
0x00007f3e9f89512a <+42> : cmp %rcx,%rdi
0x00007f3e9f89512d <+45> : jb 0x7f3e9f895147 <_dl_update_slotinfo+71>
0x00007f3e9f89512f <+47> : mov %rcx,%r9
0x00007f3e9f895132 <+50> : nopw 0x0(%rax,%rax,1)
0x00007f3e9f895138 <+56> : mov 0x8(%rsi),%rsi
0x00007f3e9f89513c <+60> : sub %r9,%rdx
0x00007f3e9f89513f <+63> : mov (%rsi),%r9
0x00007f3e9f895142 <+66> : cmp %rdx,%r9
0x00007f3e9f895145 <+69> : jbe 0x7f3e9f895138 <_dl_update_slotinfo+56>
0x00007f3e9f895147 <+71> : add $0x1,%rdx 
```

```asm
0x00007f3e9f89514b <+75>: shl $0x4,%rdx
0x00007f3e9f89514f <+79>: mov (%rsi,%rdx,1),%rbx
0x00007f3e9f895153 <+83>: cmp %rbx,(%r10)
0x00007f3e9f895156 <+86>: jae 0x7f3e9f8952bd <_dl_update_slotinfo+445>
0x00007f3e9f89515c <+92>: mov %rdi,0x20(%rsp)
0x00007f3e9f895161 <+97>: movq $0x0,0x18(%rsp)
0x00007f3e9f89516a <+106>: movq $0x0,0x8(%rsp)
0x00007f3e9f895173 <+115>: nop1 0x0(%rax,%rax,1)
0x00007f3e9f895178 <+120>: mov 0x18(%rsp),%rax
0x00007f3e9f89517d <+125>: xor %r13d,%r13d
0x00007f3e9f895180 <+128>: test %rax,%rax
0x00007f3e9f895183 <+131>: sete %r13b
0x00007f3e9f895187 <+135>: cmp %rcx,%r13
0x00007f3e9f89518a <+138>: jae 0x7f3e9f895290 <_dl_update_slotinfo+400>
0x00007f3e9f895190 <+144>: shl $0x4,%rax
0x00007f3e9f895194 <+148>: mov %rax,0x28(%rsp)
0x00007f3e9f895199 <+153>: jmpq 0x7f3e9f895226 <_dl_update_slotinfo+294>
0x00007f3e9f89519e <+158>: xchg %ax,%ax
0x00007f3e9f8951a0 <+160>: mov 0x18(%rsp),%rax
0x00007f3e9f8951a5 <+165>: mov 0x450(%r12),%rbp
0x00007f3e9f8951ad <+173>: lea (%rax,%r13,1),%rcx
--Type <RET> for more, q to quit, c to continue without paging--
0x00007f3e9f8951b1 <+177>: cmp %rcx,%rbp
0x00007f3e9f8951b4 <+180>: jne 0x7f3e9f8952c8 <_dl_update_slotinfo+456>
0x00007f3e9f8951ba <+186>: cmp %rbp,-0x1o(%r15)
0x00007f3e9f8951be <+190>: jae 0x7f3e9f8951de <_dl_update_slotinfo+222>
0x00007f3e9f8951c0 <+192>: mov %r15,%rdi
0x00007f3e9f8951c3 <+195>: callq 0x7f3e9f894730 <_dl_resize_dtv>
0x00007f3e9f8951c8 <+200>: mov %rax,%r15
0x00007f3e9f8951cb <+203>: cmp %rbp,-Ox1o(%rax)
0x00007f3e9f8951cf <+2o7>: jb 0x7f3e9f8952e7 <_dl_update_slotinfo+487>
0x00007f3e9f8951d5 <+213>: mov %rax,%fs:Ox8
0x00007f3e9f8951de <+222>: mov %rbp,%rcx
0x00007f3e9f8951e1 <+225>: shl $Ox4,%rcx
0x00007f3e9f8951e5 <+229>: add %r15,%rcx
0x00007f3e9f8951e8 <+232>: mov 6X(%)rx,%,rdi
OxOoOoOoF3eFgFgFgFgFgFgFgFgFgFgFgFgFgFgFgFgFgFgFgFgFgFgFgFgFgFgFgFgFgFgFgFgFgFgFgFgFgFgFgFgFgFgFgFgFgFgFgFgFgFgFG
OxOoOoOoF3eEef8951fc<++241>: callq 6X7f3eEF884D8O <free@plt>
OxOoOoOoF3eEF8951fc6 <++246>: mov 6X1o(%rsp),%rcx
OxOoOoOoF3eEF8951fb <++251>: cmp %rbp, OX2o(%rsp)
OxOoOoOoF3eEF8952DODCMOVNE 6X8(%rsp),%r12
OxOoOoOoF3eEF8952DODCMOVQ $Oxxxxxxxxxxxx,(%rcx)
OxOoOoOoF3eEF8952DODCMOVQ $OxO,OX8(%rcx)
OxOoOoOoF3eEF8952DODCMOVQ %r12,OX8(%rsp)
OxOoOoOoF3eEF8952AIDCMOVQ (%r4),%rcx
OxOoOoOoF3eEF8952DODCMOVQ $OxI,Y,R,R,R,R,R,R,R,R,R,R,R,R,R,R,R,R,R,R,R,R,R,R,R,R,R,R,R,R,R,R,R,R,R,R,R,R,R,R,R,R,R,R,R,R,R,R,R,R,R,R,R,R,R,R,R,R,R,R,R,R,R,R,R,R,R,R,R,R,R,R,R,R,R,R,R,R,R,R,R,R,R,R,RR,
OxOoOoOoF3eEF8952DODCMOVQ 6X7f3eEF8952DODCMOVQ (%r4),%rcx
OxOoOoOoF3eEF8952DODCMOVQ %r12,%rcx
OxOoOoOoF3eEF8952DODCMOVQ 6X7f3eEF8952DODCMOVQ (%r4),%rcx
OxOoOoOoF3eEF8952DODCMOVQ %r12,%rcx
OxOoOoOoF3eEF8952DODCMOVQ 6X6TcEefSslSlSlSlSlSlSlSlSlSlSlSlSlSlSlSlSlSlSlSlSlSlSlSlSlSlSlSlSlSlSlSlSlSlSlSlSlSlSlSlSlSlSlSlSlSlSlSlSlSlSlSlSlSlSlSlSlSlSlSlSlSlSlSlSlSlSlSlSlSlSlSlSlSlSlSlSlSlSlSlSlSlSlSlSlSlSlSlSlSlSlSlSlSlSlSlSlSlSlSlSlSLSLSLSLSLSLSLSLSLSLSLSLSLSLSLSLSLSLSLSLSLSLSLSLSLSLSLSLSLSLSLSLSLSLSLSLSLSLSLSLSLSLSLSLSLSLSLSLSLSLSLSLSLSLSLSLSLSLSLSLSLSLSLSLSLSLSLSLSLSLSLSLSLSLSLSLSLSLSLSLSLSLSLSLSLSLSLSLSLSLSLSLSLSLSLSLSLSLSLSLLSL SLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCLCL CLCL CLCL CLCL CLCL CLCL CLCL CLCL CLCL CLCL CLCL CLCL CLCL CLCL CLCL CLCL CLCL CLCL CLCL CLCL CLCL CLCL CLCL CLCL CLCL CLCL CLCL CLCL CLCL CLCL CLCL CLCL CLCL CLCL CLCL CLCL CLCL CLCL CLCL CLCL CLCL CLCL CLCL CLCL CLCL CLCL CLCL CLCL CLCL CLCL CLCL Clcl Ccl Ccl Ccl Ccl Ccl Ccl Ccl Ccl Ccl Ccl Ccl Ccl Ccl Ccl Ccl Ccl Ccl Ccl Ccl Ccl Ccl Ccl Ccl Ccl Ccl Ccl Ccl Ccl Ccl Ccl Ccl Ccl Ccl Ccl Ccl Ccl Ccl Ccl Ccl Ccl Ccl Ccl Ccl Ccl Ccl Ccl Ccl Ccl Ccl Ccl Cl cl cl cl cl cl cl cl cl cl cl cl cl cl cl cl cl cl cl cl cl cl cl cl cl cl cl cl cl cl cl cl cl cl cl cl cl cl cl cl cl cl cl cl cl cl cl cl cl cl cl cl cl cl cl cl cl cl cl cl cl cl cl cl cl cl cl cl cl cl cl cl cl cl cl cll cll cll cll cll cll cll cll cll cll cll cll cll cll cll cll cll cll cll cll cll cll cll cll cll cll cll cll cll cll cll cll cll cll cll cll cll cll cll cll cll cll cll cll cll cll cll cll cll cll cill cill cill cill cill cill cill cill cill cill cill cill cill cill cill cill cill cill cill cill cill cill cill cill cill cill cill cill cill cill cill cill cill cill cill cill cill cill cill cill cill cill cill cill cill cill cill cill cill cill cll cill cill cill cill cill cill cill cill cill cill cill cill cill cill cill cill cill cill cill cill cill cill cill cill cill cillcillcillcillcillcillcillcillcillcillcillcillcillcillcillcillcillcillcillcillcillcillcillcillcillcillcillcillcillcillcillcillcillcillcillcillcillcillcillcillcillcillcillcillcillcillcillcillc- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
		#.
		#.
		#.
		#.
		#.
		#.
		#.
		#.
		#.
		#.
		#.
		#.
		#.
		#.
		#.
		#.
		#.
		#.
		#.
		#.
		#.
		#.
		#.
		#.
		#.
		#;
		#.
		#.
		#.
		#.
		#.
		#.
		#.
		#.
		#.
		#.
		#.
		#.
		#.
		#.
		#.
		#.
		#.
		#.
			#.
			#.
			#.
			#.
			#.
			#.
			#.
			#.
			#.
			#.
			#.
			#.
			#.
			#.
			#.
			#.
			#.
			#.
			#.
			#.
			#.
			#.
			#.
			#.
			#.
				#.
				#.
				#.
				#.
				#.
				#.
				#.
				#.
				#.
				#.
				#.
				#.
				#.
				#.
				#.
				#.
				#.
				#.
				#.
				#.
				#.
				#.
				#.
				#.
				#.
			- + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + = 6.6%
			if (6.6) > 6.6:
					if (6.6) > 6.6:
						if (6.6) > 6.6:
							if (6.6) > 6.6:
								if (6.6) > 6.6:
									if (6.6) > 6.6:
										if (6.6) > 6.6:
										if (6.6) > 6.6:
										if (6.6) > 6.6:
										if (6.6) > 6.6:
										if (6.6) > 6.6:
										if (6.6) > 6.6:
										if (6.6) > 6.6:
										if (6.6) > 4.4:
										if (6.6) > 4.4:
										if (6.6) > 4.4:
										if (6.6) > 4.4:
										if (6.6) > 4.4:
										if (6.6) > 4.4:
										if (6.6) > 4.4:
										if (6.6) > 4.4:
										if (6.6) = 4.4:
										if (6.6) = 4.4:
										if (6.6) = 4.4:
										if (6.6) = 4.4:
										if (6.6) = 4.4:
										if (6.6) = 4.4:
										if (6.6) = 4.4:
										if (6.6) = 4.4:
										if (&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&& 
```

```txt
0x00007f3e9f895253 <=339>: cmp %rcx, -0x10(%r15)  
0x00007f3e9f895257 <=343>: jb 0x7f3e9f89521a <_dl_update_slotinfo+282>  
0x00007f3e9f895259 <=345>: add 0x28(%rsp),%rsi  
0x00007f3e9f89525e <=350>: add $0x1,%r13  
0x00007f3e9f895262 <=354>: lea (%r15,%rsi,1),%rbp  
0x00007f3e9f895266 <=358>: mov 0x8(%rbp),%rdi  
0x00007f3e9f89526a <=362>: callq 0x7f3e9f884080 <free@plt>  
--Type <RET> for more, q to quit, c to continue without paging--  
0x00007f3e9f89526f <=367>: mov (%r14),%rcx  
0x00007f3e9f895272 <=370>: movq $0xxxxcccccccc, 0x0(%rbp)  
0x00007f3e9f89527a <=378>: movq $0x0, 0x8(%rbp)  
0x00007f3e9f895282 <=386>: cmp %r13,%rcx  
0x00007f3e9f895285 <=389>: ja 0x7f3e9f895226 <_dl_update_slotinfo+294>  
0x00007f3e9f895287 <=391>: nopw 0x0(%rax,%rax,1)  
0x00007f3e9f895290 <=400>: mov 0x8(%r14),%r14  
0x00007f3e9f895294 <=404>: add %rcx, 0x18(%rsp)  
0x00007f3e9f895299 <=409>: test %r14,%r14  
0x00007f3e9f89529c <=412>: je 0x7f3e9f8952a6 <_dl_update_slotinfo+422>  
0x00007f3e9f89529e <=414>: mov (%r14),%rcx  
0x00007f3e9f8952a1 <=417>: jmpq 0x7f3e9f895178 <_dl_update_slotinfo+120>  
0x00007f3e9f8952a6 <=422>: mov %rbx, (%r15)  
0x00007f3e9f8952a9 <=425>: mov 0x8(%rsp),%rax  
0x00007f3e9f8952ae <=430>: add $0x38,%rsp  
0x00007f3e9f8952b2 <=434>: pop %rbx  
0x00007f3e9f8952b3 <=435>: pop %rbp  
0x00007f3e9f8952b4 <=436>: pop %r12  
0x00007f3e9f8952b6 <=438>: pop %r13  
0x00007f3e9f8952b8 <=440>: pop %r14  
0x00007f3e9f8952ba <=442>: pop %r15  
0x00007f3e9f8952bc <=444>: retq  
0x00007f3e9f8952bd <=445>: movq $0xo, 0x8(%rsp)  
0x00007f3e9f8952c6 <=454>: jmp 0x7f3e9f8952a9 <_dl_update_slotinfo+425>  
0x00007f3e9f8952c8 <=456>: lea 0x111f1(%rip),%rcx # 0x7f3e9f8a64c< PRETTY_FUNCTION>.9851>  
$X: MOV $X:BX, %edx  
X: MOV X:BX, %Rsi #  X:BX, %Rsi #  X:BX, %Rsi #  X:BX, %Rsi #  X:BX, %Rsi #  X:BX, %Rsi #  X:BX, %Rsi #  X:BX, %Rsi #  X:BX, %Rsi #  X:BX, %Rsi #  X:BX, %Rsi #  X:BX, %Rsi # X:BX, %Rsi #  X:BX, %Rsi #  X:BX, %Rsi #  X:BX, %Rsi #  X:BX, %Rsi #  X:BX, %Rsi #  X:BX, %Rsi #  X:BX, %Rsi #  X:BX, %Rsi #  X:BX, %Rsi #  X:BX, %Rsi #  X;MOV $X:BX, %edx  
X: MOV X:BX, %Rsi #  X:BX, %Rsi #  X:BX, %Rsi #  X:BX, %Rsi #  X:BX, %Rsi #  X:BX, %Rsi #  X:BX, %Rsi #  X:BX, %Rsi #  X:BX, %Rsi #  X:BX, %Rsin #  X:BX, %Rsin #  X:BX, %Rsin #  X:BX, %Rsin #  X:BX, %Rsin #  X:BX, %Rsin #  X:BX, %Rsin #  X:BX, %Rsin #  X:BX, %Rsin #  X:BX, %Rsin #  X:BX, %Rsin #  X:BX, %Rsin 
```

6. Find the section where free@plt is located, the real free address is stored, and its shared library:

```txt
(gdb) disassemble 0x7f3e9f884080  
Dump of assembler code for function free@plt:  
0x00007f3e9f884080 <=0>: jmpq *0x26f6a(%rip) # 0x7f3e9f8aaff0  
0x00007f3e9f884086 <=6>: xchg %ax,%ax  
End of assembler dump. 
```

```txt
(gdb) x/a 0x7f3e9f8aaff0  
0x7f3e9f8aaff0: 0x7f3e9f707b60 <__GI__libc_free> 
```

Symbols from "/home/coredump/LAPI/x64/bash".

Local core dump file:

(gdb) info files   
`/home/coredump/LAPI/x64/core.9', file type elf64-x86-64.  
0x000055e16515d000 - 0x000055e165160000 is load1  
0x000055e165160000 - 0x000055e165169000 is load2  
0x000055e165169000 - 0x000055e165173000 is load3  
0x000055e1667f5000 - 0x000055e166879000 is load4  
0x00007f3e9f388000 - 0x00007f3e9f389000 is load5  
0x00007f3e9f389000 - 0x00007f3e9f38a000 is load6  
0x00007f3e9f38a000 - 0x00007f3e9f390000 is load7  
0x00007f3e9f680000 - 0x00007f3e9f683000 is load8  
0x00007f3e9f839000 - 0x00007f3e9f83d000 is load9  
0x00007f3e9f83d000 - 0x00007f3e9f83f000 is load10  
0x00007f3e9f83f000 - 0x00007f3e9f843000 is load11  
0x00007f3e9f846000 - 0x00007f3e9f847000 is load12  
0x00007f3e9f847Doo - $\text{O}$ xoOoOo7f3e9f848Doo is load13 $\text{O}$ xoOoOo7f3e9f871Doo - $\text{O}$ xoOoOo7f3e9f875Doo is load14 $\text{O}$ xoOoOo7f3e9f875Doo - $\text{O}$ xoOoOo7f3e9f876Doo is load15 $\text{O}$ xoOoOo7f3e9f876Doo - $\text{O}$ xoOoOo7f3e9f878Doo is load16 $\text{O}$ xoOoOo7f3e9f8aaDoo - $\text{O}$ xoOoOo7f3e9f8abDoo is load17 $\text{O}$ xoOoOo7f3e9f8abDoo - $\text{O}$ xoOoOo7f3e9f8acDoo is load18 $\text{O}$ xoOoOo7f3e9f8acDoo - $\text{O}$ xoOoOo7f3e9f8adDoo is load19 $\text{O}$ xoOoOo7ffcbd642Doo - $\text{O}$ xoOoOo7ffcbd663Doo is load2D $\text{O}$ xoOoOo7ffcbd7eaDoo - $\text{O}$ xoOoOo7ffcbd7ebDoo is load21

Local exec file:

`/home/coredump/LAPI/x64/bash', file type elf64-x86-64. Entry point: 0x55e16507a630

0x000055e16504b2a8 - 0x000055e16504b2c4 is .interp 0x000055e16504b2c4 - 0x000055e16504b2e4 is .note.ABI-tag 0x000055e16504b2e4 - 0x000055e16504b308 is .note.gnu.build-id 0x000055e16504b308 - 0x000055e16504fdb0 is .gnu.hash

0x000055e16504fdb0 - 0x000055e16505e1b0 is .dynsym 0x000055e16505e1b0 - 0x000055e1650678d8 is .dynstr

0x000055e1650678d8 - 0x000055e165068bd8 is .gnu.version 0x000055e165068bd8 - 0x000055e165068ca8 is .gnu.version_r

0x000055e165068ca8 - 0x000055e165076928 is .rela.dyn

0x000055e165076928 - 0x000055e165077d98 is .rela.plt

0x000055e165078000 - 0x000055e165078017 is .init

0x000055e165078020 - 0x000055e165078dd0 is .plt

0x000055e165078dd0 - 0x000055e165078de8 is .plt.got

0x000055e165078df0 - 0x000055e165125781 is .text

0x000055e165125784 - 0x000055e16512578d is .fini

0x000055e165126000 - 0x000055e16513f930 is .rodata

0x000055e16513f930 - 0x000055e165143df4 is .eh_frame_hdr

0x000055e165143df8 - 0x000055e16515b730 is .eh_frame

0x000055e16515d3f0 - 0x000055e16515d3f8 is .init_array

0x000055e16515d3f8 - 0x000055e16515d400 is .fini_array

0x000055e16515d400 - 0x000055e16515fcf0 is .data.rel.ro

--Type

<RET> for more, q to quit, c to continue without paging-- 0x000055e16515fcf0 - 0x000055e16515fef0 is .dynamic

0x000055e16515fef0 - 0x000055e16515fff0 is .got

0x000055e165160000 - 0x000055e1651606e8 is .got.plt

0x000055e165160700 - 0x000055e165168d04 is .data

0x000055e165168d20 - 0x000055e165172998 is .bss

0x00007f3e9f848238 - 0x00007f3e9f84825c is .note.gnu.build-id in /lib/x86_64-linux-gnu/libtinfo.so.6

0x00007f3e9f848260 - 0x00007f3e9f848a78 is .gnu.hash in /lib/x86_64-linux-gnu/libtinfo.so.6

0x00007f3e9f848a78 - 0x00007f3e9f84a7a0 is .dynsym in /lib/x86_64-linux-gnu/libtinfo.so.6

0x00007f3e9f84a7a0 - 0x00007f3e9f84b71a is .dynstr in /lib/x86_64-linux-gnu/libtinfo.so.6

0x00007f3e9f84b71a - 0x00007f3e9f84b988 is .gnu.version in /lib/x86_64-linux-gnu/libtinfo.so.6

0x00007f3e9f84b988 - 0x00007f3e9f84bd34 is .gnu.version_d in /lib/x86_64-linux-gnu/libtinfo.so.6

0x00007f3e9f84bd38 - 0x00007f3e9f84bda8 is .gnu.version_r in /lib/x86_64-linux-gnu/libtinfo.so.6

0x00007f3e9f84bda8 - 0x00007f3e9f854d48 is .rela.dyn in /lib/x86_64-linux-gnu/libtinfo.so.6

0x00007f3e9f854d48 - 0x00007f3e9f855ae0 is .rela.plt in /lib/x86 64-linux-gnu/libtinfo.so.6

0x00007f3e9f856000 - 0x00007f3e9f856017 is .init in /lib/x86_64-linux-gnu/libtinfo.so.6

0x00007f3e9f856020 - 0x00007f3e9f856940 is .plt in /lib/x86_64-linux-gnu/libtinfo.so.6

0x00007f3e9f856940 - 0x00007f3e9f856950 is .plt.got in /lib/x86_64-linux-gnu/libtinfo.so.6

0x00007f3e9f856950 - 0x00007f3e9f863dc8 is .text in /lib/x86_64-linux-gnu/libtinfo.so.6

0x00007f3e9f863dc8 - 0x00007f3e9f863dd1 is .fini in /lib/x86_64-linux-gnu/libtinfo.so.6

0x00007f3e9f864000 - 0x00007f3e9f86d9a5 is .rodata in /lib/x86_64-linux-gnu/libtinfo.so.6

0x00007f3e9f86d9a8 - 0x00007f3e9f86e13c is .eh_frame_hdr in /lib/x86_64-linux-gnu/libtinfo.so.6

0x00007f3e9f86e140 - 0x00007f3e9f8706b0 is .eh_frame in /lib/x86_64-linux-gnu/libtinfo.so.6

0x00007f3e9f8718d0 - 0x00007f3e9f8718d8 is .init_array in /lib/x86_64-linux-gnu/libtinfo.so.6

0x00007f3e9f8718d8 - 0x00007f3e9f8718e0 is .fini_array in /lib/x86_64-linux-gnu/libtinfo.so.6

0x00007f3e9f8718e0 - 0x00007f3e9f874848 is .data.rel.ro in /lib/x86_64-linux-gnu/libtinfo.so.6

<table><tr><td>0x00007f3e9f874848 - 0x00007f3e9f874a58 is .dynamic in /lib/x86_64-linux-gnu/libtinfo.so.6</td><td></td></tr><tr><td>0x00007f3e9f874a58 - 0x00007f3e9f874ff8 is .got in /lib/x86_64-linux-gnu/libtinfo.so.6</td><td></td></tr><tr><td>0x00007f3e9f875000 - 0x00007f3e9f87544 is .data in /lib/x86_64-linux-gnu/libtinfo.so.6</td><td></td></tr><tr><td>0x00007f3e9f875500 - 0x00007f3e9f875980 is .bss in /lib/x86_64-linux-gnu/libtinfo.so.6</td><td></td></tr><tr><td>0x00007f3e9f843238 - 0x00007f3e9f84325c is .note.gnu.build-id in /lib/x86_64-linux-gnu/libd1.so.2</td><td></td></tr><tr><td>0x00007f3e9f84325c - 0x00007f3e9f84327c is .note.ABI-tag in /lib/x86_64-linux-gnu/libd1.so.2</td><td></td></tr><tr><td>0x00007f3e9f843280 - 0x00007f3e9f843348 is .gpu hash in /lib/x86_64-linux-gnu/libd1.so.2</td><td></td></tr><tr><td>0x00007f3e9f843348 - 0x00007f3e9f843750 is .dynsym in /lib/x86_64-linux-gnu/libd1.so.2</td><td></td></tr><tr><td>0x00007f3e9f843750 - 0x00007f3e9f843989 is .dynstr in /lib/x86_64-linux-gnu/libd1.so.2</td><td></td></tr><tr><td>0x00007f3e9f84398a - 0x00007f3e9f8439e0 is .gpu version in /lib/x86_64-linux-gnu/libd1.so.2</td><td></td></tr><tr><td>0x00007f3e9f8439e0 - 0x00007f3e9f84384 is .gpu version_d in /lib/x86_64-linux-gnu/libd1.so.2</td><td></td></tr><tr><td>0x00007f3e9f84388 - 0x00007f3e9f843ae8 is .gpu version_r in /lib/x86_64-linux-gnu/libd1.so.2</td><td></td></tr><tr><td>0x00007f3e9f843ae8 - 0x00007f3e9f843c80 is .rela.dyn in /lib/x86_64-linux-gnu/libd1.so.2</td><td></td></tr><tr><td>0x00007f3e9f843c80 - 0x00007f3e9f843db8 is .rela.plt in /lib/x86_64-linux-gnu/libd1.so.2</td><td></td></tr><tr><td>0x00007f3e9f844000 - 0x00007f3e9f844017 is .init in /lib/x86_64-linux-gnu/libd1.so.2</td><td></td></tr><tr><td>0x00007f3e9f844020 - 0x00007f3e9f844100 is .plt in /lib/x86_64-linux-gnu/libd1.so.2</td><td></td></tr><tr><td>0x00007f3e9f844100 - 0x00007f3e9f844128 is .plt.got in /lib/x86_64-linux-gnu/libd1.so.2</td><td></td></tr><tr><td>0x00007f3e9f844130 - 0x00007f3e9f844eb5 is .text in /lib/x86_64-linux-gnu/libd1.so.2</td><td></td></tr><tr><td>0x00007f3e9f844eb8 - 0x00007f3e9f844ec1 is .fini in /lib/x86_64-linux-gnu/libd1.so.2</td><td></td></tr><tr><td>0x00007f3e9f845000 - 0x00007f3e9f8450a3 is .rodata in /lib/x86_64-linux-gnu/libd1.so.2</td><td></td></tr><tr><td>--Type &lt;RET&gt; for more, q to quit, c to continue without paging--</td><td></td></tr><tr><td>0x00007f3e9f8450a4 - 0x00007f3e9f845178 is .eh_frame hdr in /lib/x86_64-linux-gnu/libd1.so.2</td><td></td></tr><tr><td>0x00007f3e9f845178 - 0x00007f3e9f8454f0 is .eh_frame in /lib/x86_64-linux-gnu/libd1.so.2</td><td></td></tr><tr><td>0x00007f3e9f8454f0 - 0x00007f3e9f8456a8 is .hash in /lib/x86_64-linux-gnu/libd1.so.2</td><td></td></tr><tr><td>0x00007f3e9f846d70 - 0x00007f3e9f846d80 is .init_array in /lib/x86_64-linux-gnu/libd1.so.2</td><td></td></tr><tr><td>0x00007f3e9f846d80 - 0x00007f3e9f846d90 is .fini_array in /lib/x86_64-linux-gnu/libd1.so.2</td><td></td></tr><tr><td>0x00007f3e9f846d90 - 0x00007f3e9f846fa0 is .dynamic in /lib/x86_64-linux-gnu/libd1.so.2</td><td></td></tr><tr><td>0x00007f3e9f846fa0 - 0x00007f3e9f847000 is .got in /lib/x86_64-linux-gnu/libd1.so.2</td><td></td></tr><tr><td>0x00007f3e9f847000 - 0x00007f3e9f847080 is .got.plt in /lib/x86_64-linux-gnu/libd1.so.2</td><td></td></tr><tr><td>0x00007f3e9f847080 - 0x00007f3e9f847088 is .data in /lib/x86_64-linux-gnu/libd1.so.2</td><td></td></tr><tr><td>0x00007f3e9f8470a0 - 0x00007f3e9f847110 is .bss in /lib/x86_64-linux-gnu/libd1.so.2</td><td></td></tr><tr><td>0x00007f3e9f848320 - 0x00007f3e9f8483304 is .note.gnu.build-id in /lib/x86_64-linux-gnu/libd1.so.6</td><td></td></tr><tr><td>0x00007f3e9f848334 - 0x00007f3e9f84324 is .note.ABI-tag in /lib/x86_64-linux-gnu/libd1.so.6</td><td></td></tr><tr><td>0x00007f3e9f8483328 - 0x00007f3e9f846fb0 is .gpu hash in /lib/x86_64-linux-gnu/libd1.so.6</td><td></td></tr><tr><td>0x00007f3e9f848348 - 0x00007f3e9f849cd8 is .dynsym in /lib/x86_64-linux-gnu/libd1.so.6</td><td></td></tr><tr><td>0x00007f3e9f849cd8 - 0x00007f3e9f849cd16 is .dynstr in /lib/x86_64-linux-gnu/libd1.so.6</td><td></td></tr><tr><td>0x00007f3e9f849ad16 - 0x00007f3e9f89bf84 is .gpu Version in /lib/x86_64-linux-gnu/libd1.so.6</td><td></td></tr><tr><td>0x00007f3e9f89bf88 - 0x00007f3e9f89c3b0 is .gpuVersion_d in /lib/x86_64-linux-gnu/libd1.so.6</td><td></td></tr><tr><td>0x00007f3e9f89c3b0 - 0x00007f3e9f89c3d0 is .gpuVersion_r in /lib/x86_64-linux-gnu/libd1.so.6</td><td></td></tr><tr><td>0x00007f3e9f89c3c0 - 0x00007f3e9f89c3d0 is .rela.dyn in /lib/x86_64-linux-gnu/libd1.so.6</td><td></td></tr><tr><td>0x00007f3e9f89c3d0 - 0x00007f3e9f89c3d10 is .rela.plt in /lib/x86_64-linux-gnu/libd1.so.6</td><td></td></tr><tr><td>0x00007f3e9f89c3d10 - 0x00007f3e9f89c3d20 is .plt in /lib/x86_64-linux-gnu/libd1.so.6</td><td></td></tr><tr><td>0x00007f3e9f89c3d20 - 0x00007f3e9f89c3d30 is .plt.got in /lib/x86_64-linux-gnu/libd1.so.6</td><td></td></tr><tr><td>0x00007f3e9f89c3d30 - 0x00007f3e9f89c3d40 is .text in /lib/x86_64-linux-gnu/libd1.so.6</td><td></td></tr><tr><td>0x00007f3e9f89c3d40 - 0x00007f3e9f89c3d50 is .plt.in /lib/x86_64-linux-gnu/libd1.so.6</td><td></td></tr><tr><td>0x00007f3e9f89c3d50 - 0x00007f3e9f89c3d60 is .tbss in /lib/x86_64-linux-gnu/libd1.so.6</td><td></td></tr><tr><td>0x00007f3e9f89c3d50 - 0x00007f3e9f89c3d68 is .inter_array in /lib/x86_64-linux-gnu/libd1.so.6</td><td></td></tr><tr><td>0x00007f3e9f89c3d50 - 0x00007f3e9f89c3d65 is .subc_subfreees in /lib/x86_64-linux-gnu/libd1.so.6</td><td></td></tr><tr><td>0x00007f3e9f89c3d58 - 0x00007f3e9f89c3d65 is .subc_atexit in /lib/x86_64-linux-gnu/libd1.so.6</td><td></td></tr><tr><td>0x00007f3e9f89c3d58 - 0x00007f3e9f89c3d65 is .tdata in /lib/x86_64-linux-gnu/libd1.so.6</td><td></td></tr><tr><td>0x00007f3e9f89c3d58 - 0x00007f3e9f89c3d65 is .tbss in /lib/x86_64-linux-gnu/libd1.so.6</td><td></td></tr><tr><td>0x00007f3e9f89c3d58 - 0x00007f3e9f89c3d65 is .data.in /lib/x86_64-linux-gnu/libd1.so.6</td><td></td></tr><tr><td>0x00007f3e9f89c3d58 - 0x00007f3e9f89c3d65 is .text in /lib/x86_64-linux-gnu/libd1.so.6</td><td></td></tr><tr><td>0x00007f3e9f89c3d58 - 0x00007f3e9f89c3d65 is .data.in /lib/x86_64-linux-gnu/libd1.so.6</td><td></td></tr><tr><td>0x00007f3e9f89cb8 - 0x00007f3e9f89cb48 is .data.in /lib/x86_64-linux-gnu/libd1.so.6</td><td></td></tr><tr><td>0x00007f3e9f89cb5 - 0x00007f3e9f89cb5 is .data.in /lib/x86_64-linux-gnu/libd1.so.6</td><td></td></tr><tr><td>0x00007f3e9f89cb5 - 0x00007f3e9f89cb5 is .data.in /lib/x86_64-linux-gnu/libd1.so.6</td><td></td></tr><tr><td>0x00007f3e9f89cb5 - c to quit, c to continue without paging--</td><td></td></tr><tr><td>0x00007f3e9f883430 - 0x00007f3e9f883760 is .dynsym in /lib64/ld-linux-x86-64.so.2</td><td></td></tr><tr><td>0x00007f3e9f883760 - 0x00007f3e9f883984 is .dynstr in /lib64/ld-linux-x86-64.so.2</td><td></td></tr><tr><td>0x00007f3e9f883984 - 0x00007f3e9f8839c8 is .gpu Version in /lib64/ld-linux-x86-64.so.2</td><td></td></tr><tr><td>0x00007f3e9f8839c8 - 0x00007f3e9f883a6c is .gpu Version d in /lib64/ld-linux-x86-64.so.2</td><td></td></tr><tr><td>0x00007f3e9f883e60 - 0x00007f3e9f883f8 is .rela.dyn in /lib64/ld-linux-x86-64.so.2</td><td></td></tr><tr><td>0x00007f3e9f883e60 - 0x00007f3e9f883f8 is .rela.plt in /lib64/ld-linux-x86-64.so.2</td><td></td></tr><tr><td>0x00007f3e9f883e60 - 0x00007f3e9f883f8 is .rela.plt in /lib64/ld-linux-x86-64.so.2</td><td></td></tr><tr><td>--Type &lt;RET&gt; for more, q to quit, c to continue without paging--</td><td></td></tr><tr><td>0x00007f3e9f883430 - 0x00007f3e9f874a58 is .dynamic in /lib64/ld-linux-x86-64.so.2</td><td></td></tr><tr><td>0x00007f3e9f883430 - 0x00007f3e9f874a58 is .dynamic in /lib64/ld-linux-x86-64.so.2</td><td></td></tr><tr><td>0x00007f3e9f883430 - 0x00007f3e9f874c56 is .dynamic in /lib64/ld-linux-x86-64.so.2</td><td></td></tr><tr><td>0x00007f3e9f883430 - 0x00007f3e9f874c66 is .dynamic in /lib64/ld-linux-x86-64.so.2</td><td></td></tr><tr><td>0x00007f3e9f883430 - 0x00007f3e9f874c66 is .dynamic in /lib64/ld-linux-x86-64.so.2</td><td></td></tr><tr><td>0x00007f3e9f884350 - 0x00007f3e9f845179 is .text in /lib/x86_64-linux-gnu/libd1.so.2</td><td></td></tr><tr><td>0x00007f3e9f845179 - 0x00007f3e9f845179 is .text in /lib/x86_64-linux-gnu/libd1.so.2</td><td></td></tr><tr><td>0x00007f3e9f845179 - 0x00007f3e9f845179 is .text in /lib/x86_64-linux-gnu/libd2.so.2</td><td></td></tr><tr><td>0x00007f3e9f845179 - 0x00007f3e9f845179 is .text in /lib/x86_64-linux-gnu/libd2.so.2</td><td></td></tr><tr><td>0x00007f3e9f845179 - 0x00007f3e9l874199 is .text in /lib/x86_64-linux-gnu/libd2.so.2</td><td></td></tr><tr><td>0x00007f3e9f845179 - 0x00007f3e9f845179 is .text in /lib/x86_64-linux-gnu/libd2.so.2</td><td></td></tr><tr><td>0x00007f3e9f918348 - 0x00007f3e9f9183518 is .text in /lib/x86_64-linux-gnu/libd2.so.2</td><td></td></tr><tr><td>0x00007f3e9f918348 - 0x00007f3e9f9183518 is .text in /lib/x86_64-linux-gnu/libd2.so.2</td><td></td></tr><tr><td>0x00007f3e9f18348 - 0x00007f3e9f183518 is .text in /lib/x86_64-linux-gnu/libd2.so.2</td><td></td></tr><tr><td>0x00007f3e9f18348 - 0x00007f3e9f183518 is .text in /lib/x86_64-linux-gnu/libd2.so.2</td><td></td></tr><tr><td>0x00007f4e9f87448 - 0x00007f3e9f874518 is .text in /lib/x86_64-linux-gnu/libd2.so.2</td><td></td></tr><tr><td>0x00007f4e9f87448 - 0x00007f3e9f84518 is .text in /lib/x86_64-linux-gnu/libd2.so.2</td><td></td></tr><tr><td>0x00007f4e9f87448 - 0x00007f3e9f84518 is .text in /lib/x86_64-linux-gnu/libd2.so.2</td><td></td></tr><tr><td>0x00007f4e918348 - 0x00007f3e9f183518 is .text in /lib/x86_64-linux-gnu/libd2.so.2</td><td></td></tr><tr><td>0x00007f4e918348 - 0x00007f3e9f183518 is .text in /lib/x86_64-linux-gnu/libd2.so.2</td><td></td></tr><tr><td>0x00007f4e91a58 - 0x00007f3e9f874518 is .text in /lib/x86_64-linux-gnu/libd2.so.2</td><td></td></tr><tr><td>0x00007f4e91a58 - 0x00007f3e9f874518 is .text in /lib/x86_64-linux-gnu/libd2.so.2</td><td></td></tr><tr><td>0x00007f4e91a71 - 0x00007f3e9f874518 is .text in /lib/x86_64-linux-gnu/libd2.so.2</td><td></td></tr><tr><td>0x00007f4e91a71 - 0x00007f3e9f874518 is .text in /lib/x86_64-linux-gnu/libd2.so.6</td><td></td></tr><tr><td>0x00007f4e91a71 - 0x00007f3e9f874518 is .text in /lib/x86_64-linux-gnu/libd2.so.6</td><td></td></tr><tr><td>0x00007f4e91a71 - 0x00007f3e9f874518 is .table in /lib/x86_64-linux-gnu/libd2.so.6</td><td></td></tr><tr><td>0x00007f4e91a71 - 0x00007f3e9f874518 is .table in /lib/x86_64-linux-gnu/libd2.so.6</td><td></td></tr><tr><td>0x00007f4e91a71 - 0x1111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111</td><td></td></tr><tr><td>--Type &lt;RET&gt; for more, q to quit, c to continue without paging--</td><td></td></tr><tr><td>0x00007f3e9f87448 - 0x00007f3e9f874518 is .dynamic in /lib/x86_64-linux-gnu/libd1.so.2</td><td></td></tr><tr><td>0x00007f3e9f87448 - 0x00007f3e9f874518 is .text in /lib/x86_64-linux-gnu/libd1.so.2</td><td></td></tr><tr><td>0x00007f3e9f87448 - 0x00007f3e9f874518 is .text in /lib/x86_64-linux-gnu/libd1.so.2</td><td></td></tr><tr><td>0x00007f3e9f87448</td><td>- 0x00007f3e9f874518 is .text in /lib/x86_64-linux-gnu/libd1.so.2</td></tr><tr><td>0x00007f3e9f87448</td><td>- 0x00007f3e9f874518 is .text in /lib/x86_64-linux-gnu/libd1.So.2</td></tr><tr><td>0x00007f3e9f87448</td><td>- 0x00007f3e9f874518 is .text in /lib/x86_64-linux-gnu/libd1.So.2</td></tr><tr><td>0x00007f3e9f87448</td><td>- 0x00007f3e9f7ef84518 is .text in /lib/x86_64-linux-gnu/libd1.So.2</td></tr><tr><td>0x00007f3e9f87448</td><td>- 0x00007f3e9f7ef84518 is .text in /lib/x86_64-linux-gnu/libd1.So.2</td></tr><tr><td>0x00007f4e9f87448</td><td>- 0x00007f3e9f7ef84518 is .text in /lib/x86_64-linux-gnu/libd1.So.2</td></tr><tr><td>0x00007f4e9f87448</td><td>- 0x111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111</td></tr><tr><td>---Type &lt;RET&gt; for more, q to quit, c to continue without paging--</td><td></td></tr><tr><td>0x00007f3e9f874435 - 0x00007f3e9f874518 is .dynamic in /lib/x86_64-linux-gnu/libd1.so.2</td><td></td></tr><tr><td>0x00007f3e9f874435 - 0x00007f3e9f874518 is .text in /lib/x86_64-linux-gnu/libd1.so.2</td><td></td></tr><tr><td>0x00007f3e9f874435 - 0x11111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111</td><td></td></tr><tr><td>-- Type &lt;RET&gt; for more, q to quit, c to continue without paging--</td><td></td></tr><tr><td>0x00007f3e9f874435 - 0x11111111111111111111111111111111111111111111111111111111111111111111111111</td><td></td></tr><tr><td>--Type &lt;RET&gt; for more, q to quit, c to continue without paging--</td><td></td></tr><tr><td>0X: 2x: 2x: 2x: 2x: 2x: 2x: 2x: 2x: 2x: 2x: 2x: 2x: 2x: 2x: 2x: 2x: 2x: 2x: 2x: 2x: 2x: 2x: 2x: 2x: 2x: 2×: 2x: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2x: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2x: 2×: 2x: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2x: 2×: 2×: 2x: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2x: 2x: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2x: 2x: 2×: 2x: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2x: 2×: 2×: 2×: 2x: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2x: 2×: 2x: 2×: 2x: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2x: 2x: 2×: 2×: 2x: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2x: 2x: 2x: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2x: 2x: 2x: 2×: 2x: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2x: 2×: 2×: 2×: 2×: 2x: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2x: 2×: 2×: 2x: 2×: 2x: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2x: 2×: 2x: 2×: 2×: 2x: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2x: 2×: 2x: 2x: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2x: 2×: 2x: 2x: 2×: 2x: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2x: 2x: 2×: 2×: 2×: 2x: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2x: 2x: 2×: 2x: 2×: 2x: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2x: 2x: 2x: 2×: 2×: 2x: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2x: 2x: 2x: 2x: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2×: 2x: 2x: 2x: 2x: 2x: 2x: 2x: 2x: 2x: 2x: 2x: 2x: 2x: 2x: 2x: 2x: 2x: 2x: 2x: 2x: 2x: 2x: 2x: 2x: 2 ×: 2x: 2x: 2x: 2x: 2x: 2x: 2x: 2x: 2x: 2x: 2x: 2x: 2x: 2x: 2x: 2x: 2x: 2x: 2x: 2x: 2x: 2x: 2x: 2x: 2 x: 2x: 2x: 2x: 2x: 2x: 2x: 2x: 2x: 2x: 2x: 2x: 2x: 2x: 2x: 2x: 2x: 2x: 2x: 2x: 2x: 2x: 2x: 2x: 2x: 2X = x;</td><td></td></tr></table>

```txt
0x00007f3e9f8a2000 - 0x00007f3e9f8a6620 is . Rodata in /lib64/ld-linux-x86-64.so.2
0x00007f3e9f8a6620 - 0x00007f3e9f8a6cf4 is .eh_frame hdr in /lib64/ld-linux-x86-64.so.2
0x00007f3e9f8a6cf8 - 0x00007f3e9f8a93dc is .eh_frame in /lib64/ld-linux-x86-64.so.2
0x00007f3e9f8aa640 - 0x00007f3e9f8aaa74 is .data.rel.ro in /lib64/ld-linux-x86-64.so.2
0x00007f3e9f8aae78 - 0x00007f3e9f8aafe8 is .dynamic in /lib64/ld-linux-x86-64.so.2
0x00007f3e9f8aafe8 - 0x00007f3e9f8aaff8 is .got in /lib64/ld-linux-x86-64.so.2
0x00007f3e9f8ab00 - 0x00007f3e9f8ab050 is .got.plt in /lib64/ld-linux-x86-64.so.2
0x00007f3e9f8ab06 - 0x00007f3e9f8abff8 is .data in /lib64/ld-linux-x86-64.so.2
0x00007f3e9f8ac00 - 0x00007f3e9f8ac190 is .bss in /lib64/ld-linux-x86-64.so.2
0x00007f3e9f37b238 - 0x00007f3e9f37b25c is .note.gnu.build-id in /lib/x86_64-linux-gnu/libnss_files.so.2
0x00007f3e9f37b25c - 0x00007f3e9f37b27c is .note.ABI-tag in /lib/x86_64-linux-gnu/libnss_files.so.2
0x00007f3e9f37b280 - 0x00007f3e9f37b59o is .gpuhash in /lib/x86_64-linux-gnu/libnss_files.so.2
0x00007f3e9f37b59o - 0x00007f3e9f37c0e8 is .dynsym in /lib/x86_64-linux-gnu/libnss_files.so.2
0x00007f3e9f37c0e8 - 0x00007f3e9f37ca5a is .dynstrin /lib/x86_64-linux-gnu/libnss_files.so.2
0x00007f3e9f37ca5a - 0x00007f3e9f37cb4c is .gpu-version in /lib/x86_64-linux-gnu/libnss_files.so.2
0x00007f3e9f37cb5b - 0x00007f3e9f37cb88 is .gpu_version_d in /lib/x86_64-linux-gnu/libnss_files.so.2
0x00007f3e9f37cb88 - 0x00007f3e9f37cbe8 is .gpu-version_r in /lib/x86_64-linux-gnu/libnss_files.so.2
0x00007f3e9f37cbe8 - 0x00007f3e9f37cdd1o is .rela.dyn in /lib/x86_64-linux-gnu/libnss_files.so.2
1X1111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111 
```

7. Find shared libraries that are dynamically linked with free functions and verify that they all use the same free function:

(gdb) info functions free@plt   
```txt
All functions matching regular expression "free@plt": Non-debugging symbols: 0x000055e165078080 free@plt 0x000055e165078370 regfree@plt 0x00007f3e9f856070 free@plt 0x00007f3e9f844040 free@plt 0x00007f3e9f6a5318 free@plt 0x00007f3e9f884080 free@plt 0x00007f3e9f37e050 free@plt 
```

Note: We check 5 entries except the one that we’ve already done.

```asm
(gdb) disassemble 0x000055e165078080   
Dump of assembler code for function free@plt: 0x000055e165078080 <+0>: jmpq *0xe7fba(%rip) # 0x55e165160040 <free@got.plt> 0x000055e165078086 <+6>: pushq $0x5 0x000055e16507808b <+11>: jmpq 0x55e165078020   
End of assembler dump.   
(gdb) x/a 0x55e165160040   
0x55e165160040 <free@got.plt>: 0x7f3e9f707b60 <__GI__libc_free>   
(gdb) disassemble 0x00007f3e9f856070   
Dump of assembler code for function free@plt: 0x00007f3e9f856070 <+0>: jmpq *0x1ea1a(%rip) # 0x7f3e9f874a90 <free@got.plt> 0x00007f3e9f856076 <+6>: pushq $0x4 0x00007f3e9f85607b <+11>: jmpq 0x7f3e9f856020   
End of assembler dump.   
(gdb) x/a 0x7f3e9f874a90   
0x7f3e9f874a90 <free@got.plt>: 0x7f3e9f707b60 <__GI__libc_free>   
(gdb) disassemble 0x00007f3e9f844040   
Dump of assembler code for function free@plt: 0x00007f3e9f844040 <+0>: jmpq *0x2fda(%rip) # 0x7f3e9f847020 <free@got.plt> 0x00007f3e9f844046 <+6>: pushq $0x1 0x00007f3e9f84404b <+11>: jmpq 0x7f3e9f844020   
End of assembler dump.   
(gdb) x/a 0x7f3e9f847020   
0x7f3e9f847020 <free@got.plt>: 0x7f3e9f844046 <free@plt+6>   
(gdb) disassemble 0x00007f3e9f6a5318   
Dump of assembler code for function free@plt: 0x00007f3e9f6a5318 <+o>: jmpq *Ox197c8a(%rip) # 0x7f3e9f83cfa8 0x00007f3e9f6a531e <+6>: xchg %ax,%ax   
End of assembler dump.   
(gdb) x/a 0x7f3e9f83cfa8   
Ox7f3e9f83cfa8: 0x7f3e9f7O7b6O <__GI__libc_free>   
(gdb) disassemble 0x00007f3e9f37eO5O   
Dump of assembler code for function free@plt: 0x00007f3e9f37eO5O <+o>: jmpq *Oxafd2(%rip) # 0x7f3e9f389O28 <free@got.plt> 0x00007f3e9f37eO56 <+6>: pushq $Ox2  OXOOOoF3e9F37eO5b <+11>: jmpq  OX7F3e9F37eO2O   
End of assembler dump.   
(gdb) x/a  OX7F3e9F389O28   
Ox7F3e9F389O28 <free@got.plt>:  OX7F3e9F7O7b6O <__GI__libc_free> 
```

Note: Checking these entries with the info files command output above, we see the following shared libraries in addition to bash:

```txt
0x00007f3e9f6a5000 - 0x00007f3e9f6a5300 is .plt in /lib/x86_64-linux-gnu/libc.so.6  
0x00007f3e9f856020 - 0x00007f3e9f856940 is .plt in /lib/x86_64-linux-gnu/libtinfo.so.6  
0x00007f3e9f37e020 - 0x00007f3e9f37e2e0 is .plt in /lib/x86_64-linux-gnu/libnss_files.so.2  
0x00007f3e9f844020 - 0x00007f3e9f844100 is .plt in /lib/x86_64-linux-gnu/libdl.so.2 
```

8. We now check what functions the libtinfo.so.6 share library imports:

0x00007f3e9f856020 - 0x00007f3e9f856940 is .plt in /lib/x86_64-linux-gnu/libtinfo.so.6

```txt
(gdb) p (0x00007f3e9f856940-0x00007f3e9f856020)/8
$1 = 292 
```

```txt
(gdb) x/292a 0x00007f3e9f856020   
0x7f3e9f856020: 0x25ff0001ea3a35ff 0x401f0f0001ea3c   
0x7f3e9f856030 <nc_export_termtype2@plt> : 0x680001ea3a25ff 0xFFFFFFfe0e9000000   
0x7f3e9f856040 <getenv@plt> : 0x1680001ea3225ff 0xFFFFFFfd0e9000000   
0x7f3e9f856050 <noraw_sp@plt> : 0x2680001ea2a25ff 0xFFFFFFfc0e9000000   
0x7f3e9f856060 <nc_get_tty_mode_sp@plt> : 0x3680001ea2225ff 0xFFFFFFfb0e9000000   
0x7f3e9f856070 <free@plt> : 0x468001ea1a25ff 0xFFFFFFfa0e9000000   
0x7f3e9f856080 <nc_setupterm@plt> : 0x568001ea1225ff 0xFFFFFF90e900000   
0x7f3e9f856090 <nc_pathlast@plt> : 0x68801ea1a25ff 0xFFFFFF8e900000   
0x7f3e9f856o a<killchar_sp@plt> : 0x76881ea1a225ff 0xFFFFFF7o9e90o9o9   
0x7f3e9f856bO <vfprintf_chk@plt> : 0x86881oe1e9fa25ff 0xFFFFFF6o9e9o9o9o9   
0x7f3e9f856cO <errno_location@plt> : 0x96881oe1e9f225ff 0xFFFFFF5o9e9o9o9o9   
0x7f3e9f856dO <strncpy@plt> : 0xa6881oe1e9ea25ff 0xFFFFFF4o9o9o9o9   
0x7f3e9f856eO <strncpy@plt> : 0xb6881oe1e9e225ff 0xFFFFFF3o9e9o9o9o9   
0x7f3e9f856fO <keybound_sp@plt> : 0xc6881oe1e9da25ff 0xFFFFFF2o9o9o9o9   
0x7f3e9f8561O <strcpy@plt> : 0xd6881oe1e9d225ff 0xFFFFFF1o9e9o9o9o9   
0x7f3e9f85611O <nc_set_no(padding@plt> : 0xe6881oe1e9ca25ff 0xFFFFFFfOooe9o9o9o9   
Ox7f3e9f85612O <nc_getenv_num@plt> : 0xf6881oe1e9c225ff 0xFFFFFFfeoEo9o9o9o9   
Ox7f3e9f85613O <delcurterm_sp@plt> : 0x1o6881oe1e9ba25ff 0xFFFFFFfeeoEo9o9o9o9   
Ox7f3e9f85614O <tgetstr_sp@plt> : 0x116881oe1e9b225ff 0xFFFFFFfedoEo9o9o9o9   
Ox7f3e9f85615O <isatty@plt> : 0x126881oe1e9aa25ff 0xFFFFFFfecOe9o9o9o9   
Ox7f3e9f85616O <fread@plt> : 0x136881oe1e9a225ff 0xFFFFFFfebOe9o9o9o9   
Ox7f3e9f85617O <baudrate@plt> : 0x146881oe1e9a25ff 0xFFFFFFfeaOe9o9o9o9   
Ox7f3e9f85618O <reset_shell_mode_sp@plt> : 0x156881oe1e99225ff 0xFFFFFFfe9o e9o9o9o 
```

<table><tr><td>0x7f3e9f856350</td><td>&lt;_nc_get Screensize@plt&gt;:</td><td>0x32680001e8aa25ff</td><td>0xFFFFFFcc0e900000</td></tr><tr><td>0x7f3e9f856360</td><td>&lt;tparm@plt&gt;:</td><td>0x33680001e8a225ff</td><td>0xFFFFFFcb0e900000</td></tr><tr><td>0x7f3e9f856370</td><td>&lt;gettimeofday@plt&gt;:</td><td>0x34680001e89a25ff</td><td>0xFFFFFFca0e900000</td></tr><tr><td>0x7f3e9f856380</td><td>&lt;def_shell_mode_sp@plt&gt;:</td><td>0x35680001e89225ff</td><td>0xFFFFFFfc90e900000</td></tr><tr><td>0x7f3e9f856390</td><td>&lt;_nc_get_table@plt&gt;:</td><td>0x36680001e88a25ff</td><td>0xFFFFFFc80e900000</td></tr><tr><td>0x7f3e9f8563a0</td><td>&lt;memset@plt&gt;:</td><td>0x37680001e88225ff</td><td>0xFFFFFFfc70e900000</td></tr><tr><td>0x7f3e9f8563b0</td><td>&lt;termname_sp@plt&gt;:</td><td>0x38680001e87a25ff</td><td>0xFFFFFFfc60e900000</td></tr><tr><td>0x7f3e9f8563c0</td><td>&lt;_nc_str_init@plt&gt;:</td><td>0x39680001e87225ff</td><td>0xFFFFFFfc50e900000</td></tr><tr><td>0x7f3e9f8563d0</td><td>&lt;__poll_chk@plt&gt;:</td><td>0x3a680001e86a25ff</td><td>0xFFFFFFfc40e900000</td></tr><tr><td>0x7f3e9f8563e0</td><td>&lt;ioctl@plt&gt;:</td><td>0x3b680001e86225ff</td><td>0xFFFFFFfc30e900000</td></tr><tr><td>0x7f3e9f8563f0</td><td>&lt;_nc_tpmr_analyze@plt&gt;:</td><td>0x3c680001e85a25ff</td><td>0xFFFFFFfc20e900000</td></tr><tr><td>0x7f3e9f856400</td><td>&lt;_nc_free_semtermype@plt&gt;:</td><td>0x3d680001e85225ff</td><td>0xFFFFFFfc10e900000</td></tr><tr><td>0x7f3e9f856410</td><td>&lt;strncat@plt&gt;:</td><td>0x3e680001e84a25ff</td><td>0xFFFFFFfc00e900000</td></tr><tr><td>0x7f3e9f856420</td><td>&lt;putp_sp@plt&gt;:</td><td>0x3f680001e84225ff</td><td>0xFFFFFFbf0e900000</td></tr><tr><td>0x7f3e9f856430</td><td>&lt;baudrate_sp@plt&gt;:</td><td>0x40680001e83a25ff</td><td>0xFFFFFFfbe0e900000</td></tr><tr><td>0x7f3e9f856440</td><td>&lt;fputc@plt&gt;:</td><td>0x41680001e83225ff</td><td>0xFFFFFFbd0e900000</td></tr><tr><td>0x7f3e9f856450</td><td>&lt;_nc_find_type_entry@plt&gt;:</td><td>0x42680001e82a25ff</td><td>0xFFFFFFbc0e900000</td></tr><tr><td>0x7f3e9f856460</td><td>&lt;calloc@plt&gt;:</td><td>0x43680001e82225ff</td><td>0xFFFFFFbb0e900000</td></tr><tr><td>0x7f3e9f856470</td><td>&lt;strcmp@plt&gt;:</td><td>0x44680001e81a25ff</td><td>0xFFFFFFba0e900000</td></tr><tr><td>0x7f3e9f856480</td><td>&lt;putc@plt&gt;:</td><td>0x45680001e81225ff</td><td>0xFFFFFFfb90e900000</td></tr><tr><td>0x7f3e9f856490</td><td>&lt;setcurterm@plt&gt;:</td><td>0x46680001e80a25ff</td><td>0xFFFFFFfb80e900000</td></tr><tr><td>0x7f3e9f8564a0</td><td>&lt;nc_next_db@plt&gt;:</td><td>0x47680001e80225ff</td><td>0xFFFFFFfb70e900000</td></tr><tr><td>0x7f3e9f8564b0</td><td>&lt;__memcpy_chk@plt&gt;:</td><td>0x48680001e7fa25ff</td><td>0xFFFFFFfb60e900000</td></tr><tr><td>0x7f3e9f8564c0</td><td>&lt;tggetflag_sp@plt&gt;:</td><td>0x49680001e7f225ff</td><td>0xFFFFFFfb50e900000</td></tr><tr><td>0x7f3e9f8564d0</td><td>&lt;strtol@plt&gt;:</td><td>0x4a680001e7ea25ff</td><td>0xFFFFFFfb40e900000</td></tr><tr><td>0x7f3e9f8564e0</td><td>&lt;_nc_set tty_mode@plt&gt;:</td><td>0x4b680001e7e225ff</td><td>0xFFFFFFfb30e900000</td></tr><tr><td>0x7f3e9f8564f0</td><td>&lt;memcpy@plt&gt;:</td><td>0x4c680001e7da25ff</td><td>0xFFFFFFfb20e900000</td></tr><tr><td>0x7f3e9f856500</td><td>&lt;_nc_putp_sp@plt&gt;:</td><td>0x4d680001e7d225ff</td><td>0xFFFFFFfb10e900000</td></tr><tr><td>0x7f3e9f856510</td><td>&lt;time@plt&gt;:</td><td>0x4e680001e7ca25ff</td><td>0xFFFFFFfb0e900000</td></tr><tr><td>0x7f3e9f856520</td><td>&lt;fileno@plt&gt;:</td><td>0x4f680001e7c225ff</td><td>0xFFFFFFaf0e900000</td></tr><tr><td>0x7f3e9f856530</td><td>&lt;intrflush_sp@plt&gt;:</td><td>0x50680001e7ba25ff</td><td>0xFFFFFFfa0e900000</td></tr><tr><td>0x7f3e9f856540</td><td>&lt;nc_initacs_sp@plt&gt;:</td><td>0x51680001e7b225ff</td><td>0xFFFFFFfad0e900000</td></tr><tr><td>0x7f3e9f856550</td><td>&lt;_xstat@plt&gt;:</td><td>0x52680001e7aa25ff</td><td>0xFFFFFFfac0e900000</td></tr><tr><td>0x7f3e9f856560</td><td>&lt;cbreak_sp@plt&gt;:</td><td>0x53680001e7a225ff</td><td>0xFFFFFFfab0e900000</td></tr><tr><td>0x7f3e9f856570</td><td>&lt;nc_tic_dir@plt&gt;:</td><td>0x54680001e79a25ff</td><td>0xFFFFFFaaa0e900000</td></tr><tr><td>0x7f3e9f856580</td><td>&lt;malloc@plt&gt;:</td><td>0x55680001e79225ff</td><td>0xFFFFFFfa90e900000</td></tr><tr><td>0x7f3e9f856590</td><td>&lt;uctrl_sp@plt&gt;:</td><td>0x56680001e78a25ff</td><td>0xFFFFFFfa80e900000</td></tr><tr><td>0x7f3e9f8565a0</td><td>&lt;fflush@plt&gt;:</td><td>0x57680001e78225ff</td><td>0xFFFFFFfa70e900000</td></tr><tr><td>0x7f3e9f8565b0</td><td>&lt;nlanginfo@plt&gt;:</td><td>0x58680001e77a25ff</td><td>0xFFFFFFfa60e900000</td></tr><tr><td colspan="4">--Type &lt;RET&gt; for more, q to quit, c to continue without paging--</td></tr><tr><td>0x7f3e9f8565c0</td><td>&lt;savetty_sp@plt&gt;:</td><td>0x59680001e77225ff</td><td>0xFFFFFF50e900000</td></tr><tr><td>0x7f3e9f8565d0</td><td>&lt;longname@plt&gt;:</td><td>0x5a680001e76a25ff</td><td>0xFFFFFF40e900000</td></tr><tr><td>0x7f3e9f8565e0</td><td>&lt;nocbreak_sp@plt&gt;:</td><td>0x5b680001e76225ff</td><td>0xFFFFFF30e900000</td></tr><tr><td>0x7f3e9f8565f0</td><td>&lt;nc fallbackback2@plt&gt;:</td><td>0x5c680001e75a25ff</td><td>0xFFFFFFfa20e900000</td></tr><tr><td>0x7f3e9f856600</td><td>&lt;noqflush_sp@plt&gt;:</td><td>0x5d680001e75225ff</td><td>0xFFFFFFfa10e900000</td></tr><tr><td>0x7f3e9f856610</td><td>&lt;qflush_sp@plt&gt;:</td><td>0x5e680001e74a25ff</td><td>0xFFFFFFfa00e900000</td></tr><tr><td>0x7f3e9f856620</td><td>&lt;del_curterm@plt&gt;:</td><td>0x5f680001e74225ff</td><td>0xFFFFFF9f0e900000</td></tr><tr><td>0x7f3e9f856630</td><td>&lt;ncwarning@plt&gt;:</td><td>0x60680001e7325aff</td><td>0xFFFFFF9e900000</td></tr><tr><td>0x7f3e9f856640</td><td>&lt;reallocation@plt&gt;:</td><td>0x61680001e73225ff</td><td>0xFFFFFF9d0e900000</td></tr><tr><td>0x7f3e9f856650</td><td>&lt;nc Flush@plt&gt;:</td><td>0x62680001e72a25ff</td><td>0xFFFFFF9c0e900000</td></tr><tr><td>0x7f3e9f856660</td><td>&lt;setlocale@plt&gt;:</td><td>0x63680001e72225ff</td><td>0xFFFFFF9b0e900000</td></tr><tr><td>0x7f3e9f856670</td><td>&lt;delay_output_sp@plt&gt;:</td><td>0x64680001e71a25ff</td><td>0xFFFFFF9a0e900000</td></tr><tr><td>0x7f3e9f856680</td><td>&lt;cfgetospeed@plt&gt;:</td><td>0x65680001e71225ff</td><td>0xFFFFFF990e900000</td></tr><tr><td>0x7f3e9f856690</td><td>&lt;nc_err_abort@plt&gt;:</td><td>0x66680001e70a25ff</td><td>0xFFFFFF980e900000</td></tr><tr><td>0x7f3e9f8566a0</td><td>&lt;nc_set_buffer_sp@plt&gt;:</td><td>0x67680001e70225ff</td><td>0xFFFFFF970e900000</td></tr><tr><td>0x7f3e9f8566b0</td><td>&lt;ncPutpFlush_sp@plt&gt;:</td><td>0x68680001e6fa25ff</td><td>0xFFFFFF960e900000</td></tr><tr><td>0x7f3e9f8566c0</td><td>&lt;nc_add_to_try@plt&gt;:</td><td>0x69680001e6f225ff</td><td>0xFFFFFF950e900000</td></tr><tr><td>0x7f3e9f8566d0</td><td>&lt;nc_basename@plt&gt;:</td><td>0x6a680001e6ea25ff</td><td>0xFFFFFF940e900000</td></tr><tr><td>0x7f3e9f856660</td><td>&lt;keyok_sp@plt&gt;:</td><td>0x6b680001e6e225ff</td><td>0xFFFFFF930e900000</td></tr><tr><td>0x7f3e9f8566f0</td><td>&lt;tcgetattr@plt&gt;:</td><td>0x6c68001e6da25ff</td><td>0xFFFFFF920e900000</td></tr></table>

```c
0x7f3e9f856700 <tcsetattr@plt>: 0x6d680001e6d225ff 0xFFFFFF910e9000000  
0x7f3e9f856710 <curs_set_sp@plt>: 0x6e680001e6ca25ff 0xFFFFFF900e9000000  
0x7f3e9f856720 <access@plt>: 0x6f680001e6c225ff 0xFFFFFF8f0e9000000  
0x7f3e9f856730 <fopen@plt>: 0x70680001e6ba25ff 0xFFFFFF8e0e9000000  
0x7f3e9f856740 <sysconf@plt>: 0x71680001e6b225ff 0xFFFFFF8d0e9000000  
0x7f3e9f856750 <_nc_name_MATCH@plt>: 0x72680001e6aa25ff 0xFFFFFF8c0e9000000  
0x7f3e9f856760 <raw_sp@plt>: 0x73680001e6a225ff 0xFFFFFF8b0e9000000  
0x7f3e9f856770 <halfdelay_sp@plt>: 0x74680001e69a25ff 0xFFFFFF8a0e9000000  
0x7f3e9f856780 <_nc_set_tty_mode_sp@plt>: 0x75680001e69225ff 0xFFFFFF890e9000000  
0x7f3e9f856790 <_nc_get_hash_table@plt>: 0x76680001e68a25ff 0xFFFFFF88o e9o9o9o9o9o9o9o9o9o9o9o9o9o9o9o9o9o9o9o9o9o9o9o9o9o9o9o9o9o9o9o9o9o9o9o9o9o9o9o9o9o9o9o9o9o9o9o9o9o9o9 o  
Ox7f3e9f8567aO <_nc_homeTerminateinfo@plt>: 0x7768001e68225ff 0xFFFFFF87o e9o9o9o9o  
Ox7f3e9f8567bO <_nc_first_db@plt>: 0x7868001e67a25ff 0xFFFFFF86o e9o9o 
```

Note: Similar information is also available from the .GOT section.

```txt
0x00007f3e9f874a58 - 0x00007f3e9f874ff8 is .got in /lib/x86_64-linux-gnu/libtinfo.so.6 
```

```txt
(gdb) p (0x00007f3e9f874ff8-0x00007f3e9f874a58)/8
$2 = 180 
```

(gdb) x/180a 0x00007f3e9f874a58   
```xml
0x7f3e9f874a58: 0x2c848 0x0  
0x7f3e9f874a68: 0x0 0x7f3e9f857f60 <_nc_copy TERMtype2>  
0x7f3e9f874a78 <getenv@got.plt>: 0x55e1650f87b0 <getenv> 0x7f3e9f85ae50 <qiflush>  
0x7f3e9f874a88 <_nc_get_tty_mode_sp@got.plt>: 0x7f3e9f860da0 0x7f3e9f707b60 <GI_libc_free>  
0x7f3e9f874a98 <_nc_setupterm@got.plt>: 0x7f3e9f85ba70 <_nc Locale_breaksacs+352> 0x7f3e9f856a10 <_nc_is_abs_path>  
0x7f3e9f874aa8 <killchar_sp@got.plt>: 0x7f3e9f859f20 <erasechar> 0x7f3e9f78b250 <vfprintf_chk>  
0x7f3e9f874ab8 <errno_location@got.plt>: 0x7f3e9f6a7330 <GI_errno_location> 0x7f3e9f7205d0 <strncpy_sse2_unaligned>  
0x7f3e9f874ac8 <strncpy@got.plt>: 0x7f3e9f7da8a0 <strncpy_avx2> 0x7f3e9f863b50  
0x7f3e9f874ad8 <strncpy@got.plt>: 0x7f3e9f71ffa0 <strncpy_sse2_unaligned> 0x7f3e9f860510 <_nc_out wrapper+32>  
0x7f3e9f874ae8 <_nc_getenv_num@got.plt>: 0x7f3e9f859430 <use_EXTENDED_names> 0x7f3e9f859cc0 <set.curterm+16>  
0x7f3e9f874af8 <tgetstr_sp@got.plt>: 0x7f3e9f85c980 <tgetnum+16> 0x7f3e9f76ed40 <isatty>  
0x7f3e9f874b08 <freed@got.plt>: 0x7f3e9f6f3720 <GI_IO_fread> 0x7f3e9f859c00 <baudrate_sp+176>  
0x7f3e9f874b18 <reset_shell_mode_sp@got.plt>: 0x7f3e9f861060 <reset_prog_mode> 0x7f3e9f859da0 <_ncscreen_of+16>  
0x7f3e9f874b28 <setenv@got.plt>: 0x55e1650f8980 <setenv> 0x7f3e9f76d3a0 <GI_libc_write>  
0x7f3e9f874b38 <set_tabsize_sp@got.plt>: 0x7f3e9f85b3a0 <intrflush+16> 0x7f3e9f85c010 <setupterm>  
0x7f3e9f874b48 <resetty_sp@got.plt>: 0x7f3e9f861110 <savetty> 0x7f3e9f859e40 <has_IC>  
0x7f3e9f874b58 <fclose@got.plt>: 0x7f3e9f6f2910 <IO_new_fclose> 0x7f3e9f85ccf0 <ticgetflag+16>  
0x7f3e9f874b68 <_nc_doalloc@got.plt>: 0x7f3e9f859200 <_nc_first_db+1520> 0x7f3e9f859f70 <killchar>  
0x7f3e9f874b78 <strlen@got.plt>: 0x7f3e9f7def20 <_strlen_avx2> 0x7f3e9f859ed0 <has-il>  
0x7f3e9f874b88 <tputs_sp@got.plt>: 0x7f3e9f860890 <_nc_putchar> 0x7f3e9f860ff70 <def_shell_mode> 
```

0x7f3e9f874b98 <tigetstr_sp@got.pl>: 0x7f3e9f85ce20 <tigetnum+16> 0x7f3e9f856a60 <nc_rootname>   
0x7f3e9f874b8 <stack_chk_fail@got.pl>: 0x7f3e9f78d350 <stack_chk FAIL> 0x7f3e9f85a780 <typehead+16>   
0x7f3e9f874bb8 <tcfush@got.pl>: 0x7f3e9f7728c0 <tcfush> 0x7f3e9f7de930 <_strchr_avx2>   
0x7f3e9f874bc8 <keyname_sp@got.pl>: 0x7f3e9f859ff0 <flushinp> 0x7f3e9f863b00   
0x7f3e9f874bd8 <nc_keypad@got.pl>: 0x7f3e9f85930 <curs_set+16> 0x7f3e9f860fd0 <def_prog_mode>   
0x7f3e9f874be8 <nanosleep@got.pl>: 0x7f3e9f749580 <GI_nanosleep> 0x7f3e9f7ded50 <_strchr_avx2>   
0x7f3e9f874bf8 <nc_read_file_entry@got.pl>: 0x7f3e9f8625b0 0x7f3e9f85b470 <use_tioct1>   
0x7f3e9f874c08 <tparm@got.pl>: 0x7f3e9f85d420 <nc_tparm_analyze+1232> 0x7fcbdb7ea600 <这段时间day>   
0x7f3e9f874c18 <def_shell_mode_sp@got.pl>: 0x7f3e9f860ef0<nc_set tty_mode+16> 0x7f3e9f85250   
0x7f3e9f874c28 <memset@got.pl>: 0x7f3e9f7df8e0<_memset_avx2_unaligned_ERms> 0x7f3e9f85c6b0 <tgetstr+16>   
0x7f3e9f874c38 <nc_str_init@got.pl>: 0x7f3e9f862b40 <nc_read_entry+16> 0x7f3e9f78d2e0 <poll_chk>   
0x7f3e9f874c48 <ioct1.pl>: 0x7f3e9f772fc0 <ioct1> 0x7f3e9f85cf40 <tgetstr+16>   
0x7f3e9f874c58 <nc_free_termotype@got.pl>: 0x7f3e9f859400 0x7f3e9f7238B0 <strncat_sse2_unaligned>   
0x7f3e9f874c68 <putp_sp@got.pl>: 0x7f3e9f860c40 <tputs_sp+928> 0x7f3e9f859b40   
0x7f3e9f874c78 <fputc@got.pl>: 0x7f3e9f6af9o<fputc> 0x7f3e9f858B8e<nc_find_entry+160>   
0x7f3e9f874c88 <calloc@got.pl>: 0x7f3e9f7082e<_libc_calloc> 0x7f3e9f7da460 <_strcmp_avx2>   
0x7f3e9f874c98 <putc@got.pl>: 0x7f3e9f6fb2a0<GI_IOPutc> 0x7f3e9f859cao<setcurterm_sp+128>   
0x7f3e9f874ca8 <nc_next_db@got.pl>: 0x7f3e9f858bdo<nc_last_db+4B> 0x7f3e9f7d450 <memmove_chk_avx_unaligned_ERms>   
0x7f3e9f874cab <tgetflag_sp@got.pl>: 0x7f3e9f85c6<tgetent+16> 0x7f3e9f6bed50<strtol>   
0x7f3e9f874cc8 <nc_set tty_mode@got.pl>: 0x7f3e9f860ed<nc_set tty_mode_sp+112> 0x7f3e9f7df460   
<memmove_avx_unaligned_ERms>   
0x7f3e9f874cd8 <nc_putp_sp@got.pl>: 0x7f3e9f86C0B<putp+16> 0x7fcfbd7ea30 <time>   
0x7f3e9f874ce8 <fileno@got.pl>: 0x7f3e96fa9C<GI檔案> 0x7fcfbg5B2B<noqiflush>   
0x7f3e9f874ce8 <nc_initacs_sp@got.pl>: 0x7fcfbg59A4D<ocfind_entry+16>   
0x7f3e9f874d08 <cbreak_sp@got.pl>: 0x7fcfbg5aba<raw> 0x7fcfbg5BBOO   
0x7f3e9f874d18 <malloc@got.pl>: 0x7fcfbg5TOS1<GI_1bc_maloc> 0x7fcfbg5E5BQ<NC trimmed_sgr+112O>   
--Type RET for more, q to quit, c to continue without paging--   
0x7f3e9f874dc28 <flush@got.pl>: 0x7fcfbg52dc<GI_IOFFlush> 0x7fcfbg5F5BZD2140<GI_nI_langinfo>   
0x7f3e9f874dc38 <savety_sp@got.pl>: 0x7fcfbg5B1OO<reset_shell_mode> 0x7fcfbg5A3D<keyname+16>   
0x7fcfbg5d58 <noqiflush_sp@got.pl>: 0x7fcfbg5B1BB<nobreak> 0x7fcfbg5ad5D<break>   
0x7fcfbg5d68 <delcurterm@got.pl>: 0x7fcfbg5B5D6D<delcurterm_sp+144> 0x7fcfbg5F2D<GI-libc_realloc>   
0x7fcfbg5d78 <reallocation@got.pl>: 0x7fcfbg5B6B5D<nc_ouch_sp+24D> 0x7fcfbg5B5D2<GI_setlocale>   
0x7fcfbg5d88 <setlocale@got.pl>: 0x7fcfbg5B6B6D<nc Flush_sp+16D> 0x7fcfbg5F2D2A<cfgetospeed>   
0x7fcfbg5d98 <cfgetospeed@got.pl>: 0x7fcfbg5B6B6D<ncwarning+432> 0x7fcfbg5B6A4D<nc_name_match+192>   
0x7fcfbg5dab<nc_set_buffer_sp@got.pl>: 0x7fcfbg5B5AID<has key+16> 0x7fcfbg5E6C2<nc_file_path+96>   
0x7fcfbg5dab<nc_add_to_try@got.pl>: 0x7fcfbg5E5AID<nc_pathlast+32> 0x7fcfbg5E6BbD<key_defined+16>   
0x7fcfbg5dcb<keyok_sp@got.pl>: 0x7fcfbg5FJ2D<gi_tcgatattr> 0x7fcfbg5FJ2D<ti:leaks_tinfo>   
0x7fcfbg5dcb<tcsetattr@trpl>: 0x7fcfbg5A5AID<nc_putpl_flush_sp+16> 0x7fcfbg5FJ2D<acces>   
0x7fcfbg5dcb<access@got.pl>: 0x7fcfbg5FJ2D<IO_new_fopen> 0x7fcfbg5FJ2D<GI_sysconf>   
0x7fcfbg5dcb<sysconf@got.pl>: 0x7fcfbg5E1BID<nc_first_name+12B> 0x7fcfbg5E1AID<acces+k=  
#cvvgs#sp@got.pl>: 0x7fcfbg5E1AID<icidk+c=  
#cvwgs#sp@got.pl>: 0x7fcfbg5E1AID<icidk+c=  
#cvwgs#sp@got.pl>: 0x7fcfbg5E1AID<icidk+c=  
#cvwgs#sp@got.pl>: 0x7fcfbg5E1AID<icidk+c=  
#cvwgs#sp@got.pl>: 0x7fcFBG1AID<icidk+c=  
#cvwgs#sp@got.pl>: 0XFCFBG1AID<icidk+c=  
#cvwgs#sp@got.pl>: 0XFCFBG1AID<icidk+c=  
#cvwgs#sp@got.pl>: 0XFCFBG1AID<icidk+c=  
#cvwgs#sp@got.pl>: 0XFCFBG1AID<icidk+c=  
#cvwgs#sp@got.pl*: 0XFCFBG1AID<icidk+c=  
#cvwgs#sp@got.pl*: 0XFCFBG1AID<icidk+c=  
#cvwgs#sp@got.pl*: 0XFCFBG1AID<icidk+c=  
#cvwgs#sp@got.pl*: 0XFCFBG1AID<icidk+c=  
#cvwgs# sp@got. pl*: 0XFCFBG1AID<icidk+C=  
#cvwgs# sp@got. pl*: 0XFCFBG1AID<icidk+C=  
#cvwgs# sp@got. pl*: 0XFCFBG1AID<icidk+C=  
#cvwgs# sp@got. pl*: 0XFCFBG1AID<icidk+C=  
#cvwgs# sp@got. pl*: 0XFCFBGAID<icidk+C=  
#cvwgs# sp@got. pl*: 0XFCFBGAID<icidk+C=  
#cvwgs# sp@got. pl*: 0XFCFBGAID<icidk+C=  
#cvwgs# sp@got. pl*: 0XFCFBGAID<icidk+C=  
#cvwgs# sp@got. pl*: 0XFCFBGIAID<icidk+C=  
#cvwgs# sp@got. pl*: 0XFCFBGAID<icidk+C=  
#cvwgs# sp@got. pl*: 0XFCFBGAID<icidk+C=  
#cvwgs# sp@got. pl*: 0XFCFBGAID<icidk+C=  
#cvwgs# sp@got. pl*: 0XFCFBIDAID<icidk+C=  
#cvwgs# sp@got. pl*: 0XFCFBIDAID<icidk+C=  
#cvwgs# sp@got. pl*: 0XFCFBIDAID<icidk+C=  
#cvwgs# sp@got. pl*: 0XFCFBIDAID<icidk+C=  
#cvwgs# sp@got. pl*: 0XFCFBDAID<icidk+C=  
#cvwgs# sp@got. pl*: 0XFCFBDAID<icidk+C=  
#cvwgs# sp@got. pl*: 0XFCFBDAID<icidk+C=  
#cvwgs# sp@got. pl*: 0XFCFBDAID<icidk+C=  
#cvwgs# sp@got. pl*: 0XFCFBIDAID<icidk+C=  
#cvwgs# sp@got. pl*: 0XFCFBIDAID<icidk+C=  
#cvwgs# sp@got. pl*: 0XFCFBIDAID<icidk+C=  
#cvwgs# sp@got. pl*: 0XFCFBBAID<icidk>C=  
#cvwgs# sp@got. pl*: 0XFCFBBAID<icidk>C=  
#cvwgs# sp@got. pl*: 0XFCFBBAID<icidk>C=  
#cvwgs# sp@got. pl*: 0XFCFBBAID<icidk>C=  
#cvwgs# sp@got. pl*: 0XFCFBBAID<icidk\C=  
#cvwgs# sp@got. pl*: 0XFCFBBAID<icidk>C=  
#cvwgs# sp@got. pl*: 0XFCFBBAID<icidk>C=  
#cvwgs# sp@got. pl*: 0XFCFBBAID<icidk>C=  
#cvwgs# sp@got. pl*: 0XFCFBBAID<icidk<C=  
#cvwgs# sp@got. pl*: 0XFCFBBAID<icidk>C=  
#cvwgs# sp@got. pl*: 0XFCFBBAID<icidk>C=  
#cvwgs# sp@got. pl*: 0XFCFBBAID<icidk>C=  
#cvwgs# sp@got. pl*: 0XFCFBBAID<icidk'C=  
#cvwgs# sp@got. pl*: 0XFCFBBAID<icidk>C=  
#cvwgs# sp@got. pl*: 0XFCFBBAID<icidk>C=  
#cvwgs# sp@got. pl*: 0XFCFBBAID<icidk>C=  
#cvwgs# sp@got. pl*: 0XFCFBBAID<icidk*C=  
#cvwgs# sp@got. pl*: 0XFCFBBAID<icidk>C=  
#cvwgs# sp@got. pl*: 0XFCFBBAID<icidk>C=  
#cvwgs# sp@got. pl*: 0XFCFBBAID<icidk>C=  
#cvwgs# sp@got. pl*: 0XFCFBBAID<icidk=C=  
#cvwgs# sp@got. pl*: 0XFCFBBAID<icidk>C=  
#cvwgs# sp@got. pl*: 0XFCFBBAID<icidk>C=  
#cvwgs# sp@got. pl*: 0XFCFBBAID<icidk>C=  
#cvwgs# sp@got. pl*: 0XFCFBBAID<icidk-C= $\mathrm{V}$ CTFs/Sp/Sp/Sp/Sp/Sp/Sp/Sp/Sp/Sp/Sp/Sp/Sp/Sp/Sp/Sp/Sp/Sp/Sp/Sp/Sp/Sp/Sp/Sp/Sp/Sp/Sp/Sp/Sp/Sp/Sp/Sp/Sp/Sp/Sp/Sp/Sp/Sp/Sp/Sp/Sp/Sp/Sp/Sp/Sp/Sp/Sp/Sp/Sp/Sp/Sp/ Sp/Sp/Sp/Sp/Sp/Sp/Sp/Sp/Sp/Sp/Sp/Sp/Sp/Sp/Sp/Sp/Sp/Sp/Sp/Sp/Sp/Sp/Sp/Sp/Sp/Sp/Sp/Sp/Sp/Sp/Sp/Sp/Sp/Sp/Sp/Sp/Sp/Sp/Sp/Sp/Sp/Sp/Sp/Sp/Sp/Sp

# Exercise L3 (WinDbg)

Goal: Explore shared libraries and their dependencies.

Memory Analysis Patterns: Module Collection; Coupled Modules; Value References.

Malware Analysis Patterns: Namespace.

1. Launch WinDbg and load a core dump core.19649 from the A64 directory:

```txt
Microsoft (R) Windows Debugger Version 10.0.25324.1001 AMD64 Copyright (c) Microsoft Corporation. All rights reserved. 
```

```txt
Loading Dump File [C:\LAPI\A64\core.19649] 64-bit machine not using 64-bit API 
```

```txt
********** Path validation summary ****  
Response Time (ms) Location  
Deferred svr*  
Symbol search path is: svr*  
Executable search path is:  
Generic Unix Version 0 UP Free ARM 64-bit (AArch64)  
System Uptime: not available  
Process Uptime: not available  
*** WARNING: Unable to verify timestamp for libc.so.6  
*** WARNING: Unable to verify timestamp for bash  
libc.so+0xb6734:  
0000ffff`bafa6734 d4000001svc #0 
```

2. Set the symbol path and logging to a file in case of lengthy output from some commands:

```txt
0:000> .sympath+ C:\LAPI\A64   
Symbol search path is: srv\;C:\LAPI\A64   
Expanded Symbol search path is: cache\*;SRV\*\*https://msdl.microsoft.com/download/symbols;c:\\lapi\a64   
********** Path validation summary****************** Response Time (ms) Location Deferred srv\* OK C:\LAPI\A64   
*** WARNING: Unable to verify timestamp for libc.so.6   
*** WARNING: Unable to verify timestamp for bash 
```

```txt
0:000> .reload   
....\*\*\*WARNING: Unable to verify timestamp for libc.so.6   
\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*   
\*\*WARNING: Unable to verify timestamp for bash   
\*\*WARNING: Unable to verify timestamp for bash   
\*Symbol Loading Error Summary \*S#lfuls 
```

You should also verify that your symbol search path (.sympath) is correct.

0:000> .logopen C:\LAPI\A64\L3-WinDbg.log

Opened log file 'C:\LAPI\A64\L3-WinDbg.log'

# 3. Get the list of loaded shared libraries:

0:000> lm   
```txt
start end module name   
0000aaa\bb6a0000 0000aaa\bb80a000 bash T (service symbols: ELF Export Symbols) c:\\lapi\\a64\\bash   
0000ffff\bbabb0000 0000ffff\bae07000 LC_CTYPE T (no symbols)   
0000ffff\baac07000 0000ffff\baef0000 locale_archive T (no symbols)   
0000ffff\baef0000 0000ffff\bb8e000 libc so T (service symbols: ELF Export Symbols) c:\\lapi\\a64\\libc.so.6   
0000ffff\bbb0a0000 0000ffff\bbb8e000 libtinfo so_6 T (service symbols: ELF In Memory Symbols)   
0000ffff\bbb8e3000 0000ffff\bbb8e4000 LC_NUMERIC T (no symbols)   
0000ffff\bbb8e4000 0000ffff\bbb8e5000 LC_TIME T (no symbols)   
0000ffff\bbb8e5000 0000ffff\bbb8e6000 LC_COLLATE T (no symbols)   
0000ffff\bbb8e6000 0000ffff\bbb8ed0o gconv Modules T (no symbols)   
OooFFFF\bbb8edoo 1k8 1k18o 1k18o ld/linux_aarch64.so T (service symbols: ELF Export Symbols) c:\\lapi\\a64\\ld-linux-aarch64.so.1   
OooFFFF\bbb118o 1k19o 1k19o LC_MONETARY T (no symbols)   
OooFFFF\bbb11bOoo 1k1cOoo SYS_LCMESSAGES T (no symbols)   
OooFFFF\bbb11cOoo 1k1cOoo 1k1cOoo LC_PAPER T (no symbols)   
OooFFFF\bbb11dOoo 1k1eOoo LC_NAME T (no symbols)   
OooFFFF\bbb11eOoo 1k1fOoo LC_ADDRESS T (no symbols)   
OooFFFF\bbb11fOoo 1k12Ooo LC TelefonE T (no symbols)   
OooFFFF\bbb12Ooo 1k12Ooo LC_MEASUREMENT T (no symbols)   
OooFFFF\bbb121Ooo 1k122Ooo LC_IDENITIFICATION T (no symbols)   
OoooFFFF\bbb126Ooo 1k27oOO 1k27oOO linux_vdso"So T (service symbols: ELF In Memory Symbols) 
```

# 4. Get the list of exported functions from all shared libraries having a free pattern:

0:000> x *!*free*   
```xml
0000aaaab70d324 bash!unfreeze_jobs_list = <no type information>   
0000aaaab798524 bash!rl_free_line_state = <no type information>   
0000aaaab779380 bash!rl_free_MATCHlist = <no type information>   
0000aaaab6d5280 bash!free PUSHed_string_input = <no type information>   
0000aaaab79d680 bash!free_history_entry = <no type information>   
0000aaaab791de0 bash!rl_free Undo_list = <no type information>   
0000aaaab70d310 bash!freeze_jobs_list = <no type information>   
0000aaaab7215e0 bash!free_mail_files = <no type information>   
0000aaaab7984a0 bash!rl_free Undo_list = <no type information>   
0000aaaab740e20 bash!xfree = <no type information>   
0000aaaab79cd40 bash!rl_free_history_entry = <no type information>   
0000aaaab721d70 bash!free_trap_strings = <no type information>   
0000aaaab791b80 bash!rl_free = <no type information>   
0000aaaab721fd0 bash!free_buffered_STREAM = <no type information>   
0000aaaab79cdeo bash!rl_free Saved_history_line = <no type information>   
0000aaaab779080 bash!rl_free_keymap = <no type information>   
0000ffffbafd72c0 libc.so!pkey_free = <no type information>   
0000ffffbbob14860 libc.so!xdr_free = <no type information>   
0000ffffbafab720 libc.so!globfree64 = <no type information>   
0000ffffbaf7f5aO libc.so!obstack_free = <no type information>   
0000ffffbafab72O libc.so!globfree = <no type information>   
0000ffffbafc21OO libc.so!freeaddrinfo = <no type information>   
0000ffffbaf7f5aO libc.so!obstack_free = <no type information>   
0000ffffbbob247aO libc.so!libc_freeres = <no type information>   
0000ffffbaf23c3O libc.so!freelocale = <no type information>   
0000ffffbafef3aO libc.so!if_freenameindex = <no type information>   
0000ffffbaf5cffO libc.so!IO_free_wbackup_area = <no type information>   
0000ffffbafbcc7O libc.so!regfree = <no type information>   
0000ffffbaf7dbd4 libc.so!free = <no type information>   
0000ffffbaf7dbd4 libc.so!cfree = <no type information>   
0000ffffbbbO92988 libc.so!free hook = <no type information>   
0000ffffbbaf66e3O libc.so!IO_free Backup_area = <no type information>   
0000ffffbbaf23c3O libc.so!freeloace = <no type information>   
0000ffffbbaf518O libc.so!wordfree = <no type information> 
```

```txt
0000ffff`bafc6d70 libc.so!_sched_cpufree = <no type information>  
0000ffff`baf7f6a0 libc.so!_libc_scratch_buffer_dupfree = <no type information>  
0000ffff`baff0760 libc.so!freeifaddrs = <no type information>  
0000ffff`baf7dbd4 libc.so!_libc_free = <no type information>  
0000ffff`bb0b0460 libtinfo.so_6!nc_free_entries = <no type information>  
0000ffff`bb0b0450 libtinfo.so_6!nc_free_termtype2 = <no type information>  
0000ffff`bb0b0440 libtinfo.so_6!nc_free_termtype = <no type information>  
0000ffff`bb0f11e4 ld-linux_aarch64_soldl_exception_free = <no type information> 
```

5. Find memory locations that reference the function address 0000ffff`baf7dbd4:

```batch
0:000> s-q 0000aaaaa\bb6a0000 L?FFFFFFF 0000ffff\bf7dbd4  
0000aaa\bb7ff7e8 0000ffff\bf7dbd4 0000ffff\bf23420  
0000ffff\bb08bde8 0000ffff\bf7dbd4 0000ffff\bb0934d8  
0000ffff\bb08c028 0000ffff\bf7dbd4 0000ffff\bf16ef0  
0000ffff\bb0dee68 0000ffff\bf7dbd4 0000ffff\bafa6ad0  
0000ffff\bb128b08 0000ffff\bf7dbd4 0000ffff\bf7d640 
```

Note: These can be .GOT locations referenced from .PLT.

6. Map these locations to module address ranges from the output of the lm command:

```txt
0000aaa`bb7ff7e8 - 0000aaa`bb6a0000 0000aaa`bb80a000 bash  
0000ffff`bb08bde8 - 0000ffff`baef0000 0000ffff`bb08e000 libc.so  
0000ffff`bb08c028 - 0000ffff`baef0000 0000ffff`bb08e000 libc.so  
0000ffff`bb0dee68 - 0000ffff`bb0a0000 0000ffff`bb0e0000 libtinfo.so_6  
0000ffff`bb128b08 - ? 
```

Note:: These addresses can also be inspected via the !address command:

```txt
0:000> !address 0000aaa`bb7ff7e8  
Usage: Image  
Base Address: 0000aaa`bb7fc000  
End Address: 0000aaa`bb801000  
Region Size: 00000000`00005000 (20.000 kB)  
State: 00001000 MEM_COMMIT  
Protect: 00000002 PAGE_READONLY  
Type: 00020000 MEM_PRIVATE  
Allocation Base: 0000aaa`bb7fc000  
Allocation Protect: 00000002 PAGE_READONLY  
Image Path:/usr/bin/bash  
Module Name: bash  
Loaded Image Name: bash  
Mapped Image Name:  
More info: lmvmbash  
More info: !lmi bash  
More info: In 0xaaaabb7ff7e8  
More info: !dh 0xaaaabb6a0000 
```

Content source: 1 (target), length: 1818

```txt
0:000> !address 0000ffff\bb08bde8 
```

```txt
Usage: Image  
Base Address: 0000ffff\bb088000  
End Address: 0000ffff\bb08c000 
```

```txt
Region Size: 00000000`00004000 (16.000 kB)  
State: 00001000 MEM_COMMIT  
Protect: 00000002 PAGE_READONLY  
Type: 00020000 MEM_PRIVATE  
Allocation Base: 0000ffffbb088000  
Allocation Protect: 00000002 PAGE_READONLY  
Image Path: /usr/lib/aarch64-linux-gnu/libc.so.6  
Module Name: libc.so  
Loaded Image Name: libc.so.6  
Mapped Image Name:  
More info: lmvmlibc.so  
More info: !lmi libc.so  
More info: ln 0xffffbb08bde8  
More info: !dh 0xffffbaef0000 
```

Content source: 1 (target), length: 218

0:000> !address 0000ffff`bb08c028   
```txt
Usage: Image  
Base Address: 0000ffff\bb08c000  
End Address: 0000ffff\bb08e000  
Region Size: 0000000\00002000 (8.000 kB)  
State: 00001000 MEM_COMMIT  
Protect: 0000004 PAGE_READWRITE  
Type: 00020000 MEM_PRIVATE  
Allocation Base: 0000ffff\bb08c000  
Allocation Protect: 0000004 PAGE_READWRITE  
Image Path: /usr/lib/aarch64-linux-gnu/libc.so.6  
Module Name: libc.so  
Loaded Image Name: libc.so.6  
Mapped Image Name:  
More info: lmv m libc.so  
More info: !lmi libc.so  
More info: ln 0xffffbbb08c028  
More info: !dh 0xffffbaef0000 
```

Content source: 1 (target), length: 1fd8

0:000> !address 0000ffff`bb0dee68   
```txt
Usage: Image  
Base Address: 0000ffffbb0db000  
End Address: 0000ffffbb0df000  
Region Size: 000000000004000 (16.000 kB)  
State: 00001000 MEM_COMMIT  
Protect: 00000002 PAGE_READONLY  
Type: 00020000 MEM_PRIVATE  
Allocation Base: 0000ffffbb0db000  
Allocation Protect: 0000002 PAGE_READONLY  
Image Path: /usr/lib/aarch64-linux-gnu/libtinfo.so.6.3  
Module Name: libtinfo.so_6  
Loaded Image Name: libtinfo.so.6.3  
Mapped Image Name:  
More info: lmv m libtinfo.so_6  
More info: !lmi libtinfo.so_6  
More info: In 0xffffbb0dee68  
More info: !dh 0xffffbb0a000 
```

Content source: 1 (target), length: 198

0:000> !address 0000ffff`bb128b08

```txt
Usage: <unknown>  
Base Address: 0000ffff\bb127000  
End Address: 0000ffff\bb129000  
Region Size: 00000000\00002000 (8.000 kB)  
State: 00001000 MEM_COMMIT  
Protect: 00000002 PAGE_READONLY  
Type: 00020000 MEM_PRIVATE  
Allocation Base: 0000ffff\bb127000  
Allocation Protect: 0000002 PAGE_READONLY 
```

Content source: 1 (target), length: 4f8

7. We can see what functions are imported by dumping memory with symbols around such .GOT addresses:

```txt
0:000> dps 0000ffffbb0dee68-40 L20   
0000ffffbb0dee28 0000ffffbafc7030 libc(so!stat   
0000ffffbb0dee30 0000ffffbafc7c20 libc(so!write   
0000ffffbb0dee38 0000ffffbafc7d30 libc(so!access   
0000ffffbb0dee40 0000ffffbafe3cd0 libc.so!_fprintf_chk   
0000ffffbb0dee48 0000ffffbaf80000 libc.so!strcmp   
0000ffffbb0dee50 0000ffffbaf24430 libc.so!_ctype_b_loc   
0000ffffbb0dee58 0000ffffbaf2ed74 libc.so!strtol   
0000ffffbb0dee60 0000ffffbaf59880 libc.so!fread   
0000ffffbb0dee68 0000ffffbaf7dbd4 libc.so!free   
0000ffffbb0dee70 0000ffffbafa6ad0 libc.so!nanosleep   
0000ffffbb0dee78 0000ffffbaf7ff4o libc.so!strchr   
0000ffffbb0dee8O 0oOoFfBaf59dbO libc.so!IO_fwrite   
O O O O O O O O O O O O O O O O O O O O O O O O O O O O O O O O O O O O O O O O O O O O O O O O O O O O O O O O O O O O O O O O O O O O O O O O O O O O O O O O O O O O O O O O O O O O O O O O O O O O O o   
OoOoOoOoOoOoOoOoOoOoOoOoOoOoOoOoOoOoOoOoOoOoOoOoOoOoOoOoOoOoOoOoOoOoOoOoOoOoOoOoOoOoOoOoOoOoOoOoOoOoOoo   
OOOOOOBBbbdeeaO 1111111111111111111111111111111111111111111111111111111111111111   
OOOOOOBBbbdeea8 111111111111111111111111111   
OOOOOOBBbbdeebO 111111111111111111   
OOOOOOBBbbdeeb8 11   
OOOOOOBBbbdeecO 2222222222222222222222222222222222222222222222222222222222222222222222222222   
OOOOOOBBbbdeec8 3333333333333333333333333333333333333333333333333333333333 
```

# API Usage

Module usage (static analysis)   
Hidden Module   
Function usage (dynamic analysis)

GDB Commands

(gdb) break function

(gdb) rbreak regex

2023Software Diagnostics Services

To find out whether a particular module is potentially used, we can look at Module Collection or scan in memory for Hidden Modules. To find out whether a particular module uses this or that function, we can just look at its .PLT section. It can be done statically, looking at a memory dump, for example. However, if we want to find out where in code a particular function is used, we can’t search for its address or its address in the .PLT section because usually relative addressing is used. Here we can find the location in code dynamically, for example, using a breakpoint.

# Exercise L4

Goal: Find usage of specific Linux APl functions   
Debugging Implementation Patterns: Code Breakpoint; Breakpoint Action   
\LAPI-Dumps\Exercise-L4-GDB.pdf

2023 Software Diagnostics Services

Goal: Find usage of specific Linux API functions.

Debugging Implementation Patterns: Code Breakpoint; Breakpoint Action.

1. Launch GDB with /bin/bash process to debug (you can also choose any other process for experimentation):

```txt
~/LAPI/x64$ gdb /bin/bash
GNU gdb (Debian 8.2.1-2+b3) 8.2.1
Copyright (C) 2018 Free Software Foundation, Inc.
License GPLv3+: GNU GPL version 3 or later <http://gnu.org/licenses/gpl.html>
This is free software: you are free to change and redistribute it.
There is NO WARRANTY, to the extent permitted by law.
Type "show copying" and "show warranty" for details.
This GDB was configured as "x86_64-linux-gnu".
Type "show configuration" for configuration details.
For bug reporting instructions, please see:
<http://www.gnu.org/software/gdb/bugs/>. 
Find the GDB manual and other documentation resources online at:
<http://www.gnu.org/software/gdb/documentation/>. 
For help, type "help".
Type "apropos word" to search for commands related to "word"... 
Reading symbols from /bin/bash...(no debugging symbols found)...done. 
```

2. Set logging to a file in case of lengthy output from some commands:

(gdb) set logging on L4.log Copying output to L4.log.

3. Set the breakpoint on the malloc function from libc (we are specific because there may be other functions with the same name, for example, the loader has its own version of malloc to use before glibc is loaded):

```txt
(gdb) break __libc_malloc  
Function "_libc_malloc" not defined.  
Make breakpoint pending on future shared library load? (y or [n]) y  
Breakpoint 1(_libc_malloc) pending. 
```

4. Run the program, wait for the break-in, and check the backtrace:

(gdb) run   
Starting program:/bin/bash   
Breakpoint 1, _GI__libc_malloc (bytes=5) at malloc.c:3036   
3036 malloc.c: No such file or directory.   
(gdb) bt   
#0 _GI__libc_malloc (bytes=5) at malloc.c:3036   
#1 0x00007ffff7e03ae0 in _nl_normalize_codeset (codeset=codeset@entry=0x7FFFFFFfe7fe "UTF-8", name_len $\equiv$ name_len@entry $= 5$ ) at ../intl/l10nflist.c:321   
#2 0x00007ffff7dfddb5 in _nl_load_location_from_archive (category=category@entry=12, namep $\equiv$ namep@entry $= 0x7FFFFFFfe180$ ) at loadarchive.c:173   
#3 0x00007ffff7dfcd04 in _nl_find_location (locale_path $= 0\mathrm{x}0$ , locale_path_len $= 0$ category $\equiv$ category@entry $= 12$ ,name $\equiv$ name@entry $= 0x7FFFFFFfe180$ ) at findlocale.c:153

```txt
40x00007ffff7dfc6ef in _GI_setlocale (locale=<optimized out>, category=<optimized out>) at setlocale.c:340  
#5 _GI_setlocale (category=<optimized out>, locale=<optimized out>) at setlocale.c:219  
#6 0x00005555555db502 in set_defaultlocale()  
#7 0x000055555581ee7 in main () 
```

5. We continue execution until the next breakpoint is hit:

```lisp
(gdb) continue
Continuing.
Breakpoint 1, __GI__libc_malloc (bytes=5) at malloc.c:3036
3036 in malloc.c
(gdb) bt
#0 __GI__libc_malloc (bytes=5) at malloc.c:3036
#1 0x00007ffff7e03ae0 in _nl_normalize_codeset (codeset=codeset@entry=0x7FFFFFFfe7fe "UTF-8",
name_len=name_len@entry=5) at ../int1/l10nflist.c:321
#2 0x00007ffff7dfddb5 in _nl_load_location_from_archive (category=category@entry=12,
namep=namep@entry=0x7FFFFFFfe180) at loadarchive.c:173
#3 0x00007ffff7dfcd04 in _nl_find_location (locale_path=0x0, locale_path_len=0,
category=category@entry=12, name=name@entry=0x7FFFFFFfe180)
at findlocale.c:153
#4 0x00007ffff7dfc6ef in __GI_setlocale (locale=<optimized out>, category=<optimized out>) at
setlocale.c:340
#5 __GI_setlocale (category=<optimized out>, locale=<optimized out>) at setlocale.c:219
#6 0x000055555555db502 in set_default_location()
#7 0x0000555555581ee7 in main () 
```

6. We can add an action to breakpoints to automate printing backtraces:

```txt
(gdb) info breakpoints  
Num Type Disp Enb Address What  
1 breakpoint keep y 0x00007ffff7e53510 in __GI__libc_malloc at malloc.c:3036  
breakpoint already hit 3 times 
```

```txt
(gdb) commands 1  
Type commands for breakpoint(s) 1, one per line.  
End with a line saying just "end".  
>bt 
```

```txt
>continue  
>end 
```

7. We now get a backtrace after each breakpoint hit:

```txt
(gdb) continue
Continuing.   
Breakpoint 1, _GI__libc_malloc (bytes=776) at malloc.c:3036   
3036 in malloc.c   
#0 _GI__libc_malloc (bytes=776) at malloc.c:3036   
#1 0x00007fff7dfd499 in _nl_interpora_late_data (category=category@entry=0, data=0x7fff7b005c0, datasize=337024) at loadlocale.c:98   
#2 0x00007fff7dfdd05 in _nl_loadpora_late_from_archive (category=category@entry=12, namep=namep@entry=0x7fffffe180) at loadarchive.c:477   
#3 0x00007fff7dfcd04 in _nl_findpora_late (locale_path=0x0, locale_path_len=0, category=category@entry=12, name=name@entry=0x7fffffe180) at findlocale.c:153   
#4 0x00007fff7dfc6ef in _GI_setlocale (locale=<optimized out>, category=<optimized out>) at setlocale.c:340   
#5 _GI_setlocale (category=<optimized out>, locale=<optimized out>) at setlocale.c:219   
#6 0x00005555555db502 in set_defaultpora_late ()   
#7 0x0000555555581ee7 in main ()   
Breakpoint 1, _GI__libc_malloc (bytes=112) at malloc.c:3036   
3036 in malloc.c   
#0 _GI__libc_malloc (bytes=112) at malloc.c:3036   
#1 0x00007fff7dfd499 in _nl_interpora_late_data (category=category@entry=1, data=0x7fff7dca0d0, datasize=54) at loadlocale.c:98   
#2 0x00007fff7dfdd05 in _nl_loadpora_late_from_archive (category=category@entry=12, namep=namep@entry=0x7fffffe180) at loadarchive.c:477   
#3 0x00007fff7dfcd04 in _nl_findpora_late (locale_path=0xO, locale_path_len=O, category=category@entry=12, name=name@entry=Ox7fffffe18O) at findlocale.c:153   
#4 0x00007fff7dfc6ef in _GI_setlocale (locale=<optimized out>, category=<optimized out>) at setlocale.c:340   
#5 _GI_setlocale (category=<optimized out>, locale=<optimized out>) at setlocale.c:219   
#6 0x00005555555db502 in set defaultpora_late ()   
#7 0x0000555555581ee7 in main ()   
Breakpoint 1, _GI__libc_malloc (bytes=1336) at malloc.c:3036   
3036 in malloc.c   
#o _GI__libc_malloc (bytes=1336) at malloc.c:3036   
#1 0x00007fff7dfd499 in _nl_interpora_late_data (category=category@entry=2, data=Ox7fff7dca11O, datasize=3284) at loadlocale.c:98   
#2 0x00007fff7dfdd05 in _nl_loadpora_late_from_archive (category=category@entry=12, namep=namep@entry=Ox7fffffe18O) at loadarchive.c:477   
#3 0x00007fff7dfcd04 in _nl_findpora_late (locale_path=OxO, locale_path_len=O, category=category@entry=12, name=name@entry=Ox7fffffe18O) at findlocale.c:153   
#4 0x00007fff7dfc6ef in _GI_setlocale (locale=<optimized out>, category=<optimized out>) at setlocale.c:34O   
#5 _GI_setlocale (category=<optimized out>, locale=<optimized out>) at setlocale.c:219   
#6 0x00005555555db5O2 in set_defaultpora_late ()   
#7 0x0000555555581ee7 in main ()   
Breakpoint 1, _GI__libc_malloc (bytes=216) at malloc.c:3036   
3036 in malloc.c   
#o _GI__libc_malloc (bytes=216) at malloc.c:3036 
```

```html
10x00007ffff7dfd499 in _nl_inter_location_data (category=category@entry=3, data=0x7fff7b52a40, datasize=2586242) at loadlocale.c:98
#2 0x00007ffff7dfdd05 in _nl_load_location_from_archive (category=category@entry=12, namep=namep@entry=0x7FFFFFFfe180) at loadarchive.c:477
#3 0x00007ffff7dfcd04 in _nl_find_location (locale_path=0x0, locale_path_len=0, category=category@entry=12, name=name@entry=0x7FFFFFFfe180)
at findlocale.c:153
--Type <RET> for more, q to quit, c to continue without paging--q Quit 
```

```txt
(gdb) q  
A debugging session is active.  
Inferior 1 [process 23584] will be killed.  
Quit anyway? (y or n) y 
```

# Delayed Dynamic Linking

![](images/2f116d86e92211d2b433d0be38668daa6e81be9ccbba0c1eea9e144572e4e7fa.jpg)

# First call:

open@plt:

![](images/c46d1c9806f272f3430212e0752ece8478a0c313fb2aca13e6930cfb382b72bf.jpg)

# Subsequent calls:

open@plt:

![](images/45aa6e33fd6d8947f80c4da7d007f80a536505e4b2e931698bcf6e18b42b8e94.jpg)

2023Software Diagnostics Services

If certain Linux API function categories may not be used (for example, lazy evaluation) at all during the program execution, there is no need to resolve .PLT references to the loaded shared library. You may have noticed it in the previous exercises with some LIBC functions having @plt+offset in the .GOT.PLT section as a destination. The second jump will go to a dynamic linker and replace the value to point to the real address from the shared library. We see that in the next live debugging exercise.

# Exercise L5

Goal: Explore the delayed dynamic linking   
Debugging Implementation Patterns: Code Breakpoint; Break-in   
ADDR Patterns: Call Path   
\LAPI-Dumps\Exercise-L5-GDB.pdf

2023 Software Diagnostics Services

Goal: Explore the delayed dynamic linking.

Debugging Implementation Patterns: Code Breakpoint; Break-in.

ADDR Patterns: Call Path.

1. Launch GDB with /bin/bash process to debug (you can also choose any other process for experimentation):

```txt
~/LAPI/x64$ gdb /bin/bash
GNU gdb (Debian 8.2.1-2+b3) 8.2.1
Copyright (C) 2018 Free Software Foundation, Inc.
License GPLv3+: GNU GPL version 3 or later <http://gnu.org/licenses/gpl.html>
This is free software: you are free to change and redistribute it.
There is NO WARRANTY, to the extent permitted by law.
Type "show copying" and "show warranty" for details.
This GDB was configured as "x86_64-linux-gnu".
Type "show configuration" for configuration details.
For bug reporting instructions, please see:
<http://www.gnu.org/software/gdb/bugs/>. 
Find the GDB manual and other documentation resources online at:
<http://www.gnu.org/software/gdb/documentation/>. 
For help, type "help".
Type "apropos word" to search for commands related to "word"... 
Reading symbols from /bin/bash...(no debugging symbols found)...done. 
```

2. Set logging to a file in case of lengthy output from some commands:

```txt
(gdb) set logging on L5.log Copying output to L5.log. 
```

3. Set the breakpoint on the main function and run the program to allow shared libraries to be loaded:

```txt
(gdb) break main   
Breakpoint 1 at 0x2de30   
(gdb) r   
Starting program:/bin/bash   
Breakpoint 1,0x0000555555581e30 in main () 
```

4. Disassemble the shell_execve function and follow the call path of open@plt:

```asm
(gdb) disassemble shell_execve  
Dump of assembler code for function shell_execve:  
0x0000555555987a0 <=0>: push %r15  
0x0000555555987a2 <=2>: push %r14  
0x0000555555987a4 <=4>: mov %rdx,%r14  
0x0000555555987a7 <=7>: push %r13  
0x0000555555987a9 <=9>: mov %rsi,%r13  
0x0000555555987ac <=12>: push %r12  
0x0000555555987ae <=14>: push %rbp  
0x0000555555987af <=15>: push %rbx  
0x0000555555987b0 <=16>: mov %rdi,%rbx 
```

```asm
0x0000555555987b3 ++19> : sub $0xa8,%rsp
0x0000555555987ba ++26> : mov %fs:0x28,%rax
0x0000555555987c3 ++35> : mov %rax,0x98(%rsp)
0x0000555555987cb ++43> : xor %eax
0x0000555555987cd ++45> : callq 0x555555815e0 <execve@plt>
0x0000555555987d2 ++50> : callq 0x55555581120 <__errno_location@plt>
0x0000555555987d7 ++55> : mov %rax,%r12
0x0000555555987da ++58> : mov (%rax),%ebp
0x0000555555987dc ++60> : mov 0xdc2a6(%rip),%eax # 0x555555674a88
<terminating_signal>
0x0000555555987e2 ++66> : test %eax,%eax
0x0000555555987e4 ++68> : jne 0x55555598880 <shell_execve+224>
0x0000555555987ea ++74> : cmp $0x8,%ebp
0x0000555555987ed ++77> : je 0x55555598894 <shell_execve+244>
0x0000555555987f3 ++83> : xor %eax,%eax
0x0000555555987f5 ++85> : cmp $0x2,%ebp
0x0000555555987f8 ++88> : mov %rbx,%rdi
0x0000555555987fb ++91> : sete %al
0x0000555555987fe ++94> : add $0x7e,%eax
0x000055555598801 ++97> : mov %eax,0xd321(%rip) # 0x55555676b28
<last_command_exit_value>
0x000055555598807 ++103> : callq 0x55555921a0 <file_isdir>
0x000055555988c ++108> : test %eax,%eax
0x000055555988e ++110> : jne 0x44, %ebp
0x00004444444444444444444444444444444444444444444444444444444444444444444444444444444444444444444444444444 
```

Note: We see that the call path goes back to .PLT section and then jumps to a runtime resolution function from the dl library. This means that the open function was not yet called from the code. You can recognize not yet-called functions by the pattern in .GOT/.GOT.PLT sections: ...@plt+6.

5. Continue running the program, then break-in by ^C:

```txt
(gdb) continue  
Continuing.  
[Detaching after fork from child process 23845]  
coredump@DESKTOP-IS6V2L0:~/LAPI/x64$ ^C  
Program received signal SIGINT, Interrupt.  
0x00007fff7ebfc89 in __pselect (nfds=1, readfds=0x7FFFFFFd130, writefds=0x0, exceptfds=0x0, timeout=<optimized out>,  
    sigmask=0x55555567b840 <_rl_orig_sigset>) at ../sysdeps UNIX/sysv/linux/pselect.c:69  
69 ../sysdeps UNIX/sysv/linux/pselect.c: No such file or directory.  
(gdb) 
```

6. Now we disassemble the .GOT.PLT section data again and see the real function address:

(gdb) disassemble 0x555555581aa0   
Dump of assembler code for function open@plt: 0x000055555581aa0 $< + 0>$ : jmpq \*0xe7aaa(%rip) # 0x555555669550 <open@got.plt> 0x000055555581aa6 $< + 6>$ : pushq $0xa7 0x000055555581aab $< + 11>$ : jmpq 0x55555581020   
End of assembler dump.   
(gdb) x/a 0x555555669550   
0x555555669550 <open@got.plt>: 0x7fffff7eb9010 <_Libc_open64>

Note: Now we see that sometimes in the past, when the open function was called for the first time, the dynamic linker resolved the indirect address to the actual address in libc. You can verify this by setting the breakpoint on the 0x555555581aa0 address and restarting the program:

```txt
(gdb) break *0x555555581aaa0 Breakpoint 2 at 0x555555581aa0 
```

```txt
(gdb) run  
The program being debugged has been started already.  
Start it from the beginning? (y or n) y  
Starting program: /bin/bash  
Breakpoint 1, 0x0000555555581e30 in main () 
```

```txt
(gdb) x/a 0x555555669550  
0x55555669550 <open@got.plt>: 0x55555581aa6 <open@plt+6>  
(gdb) continue  
Continuing.  
Breakpoint 2, 0x000055555581aa0 in open@plt() 
```

```txt
(gdb) x/a 0x555555669550  
0x555555669550 <open@got.plt>: 0x555555581aa6 <open@p1t+6> 
```

```txt
(gdb) continue Continuing. 
```

Breakpoint 2, 0x0000555555581aa0 in open@plt ()

(gdb) x/a 0x555555669550

0x555555669550 <open@got.plt>: 0x7ffff7eb9010 <__libc_open64>

# API Sequences (Prescriptive)

open, close   
malloc,.. free   
pthread_create,..,pthread_join   
socket,..., bind, listen, accept   
wl _surface damage, wl _surface _attach, wl surface commit

2023Software Diagnostics Services

By prescriptive API sequences, we mean how to use API functions and in what sequence. It is like a traditional approach to language grammar teaching how to use a language. The slide shows some iconic prescriptive sequences we talk about when we discuss appropriate API classes.

# API Sequences (Descriptive)

# Horizontal

Code disassembly   
Traces and logs (Thread of Activity analysis pattern)

# Vertical

. Stack trace   
Traces and logs (Fiber Bundle analysis pattern)

2023Software Diagnostics Services

Descriptive sequences are sequences that we actually see in memory dumps, traces, and logs, and also during live debugging. They may deviate from prescriptive sequences, which should trigger an investigation if detected. We can discover and analyze horizontal API sequences by disassembling code or looking at trace and log messages corresponding to particular threads, and vertical API sequences by looking at stack traces from problem threads during postmortem debugging, API usage during live debugging sessions, or looking at stack traces associated with particular trace and log messages, the so-called Fiber Bundle trace and log analysis pattern.

Thread of Activity

https://www.dumpanalysis.org/blog/index.php/2009/08/03/trace-analysis-patterns-part-7/

Fiber Bundle

https://www.dumpanalysis.org/blog/index.php/2012/09/26/trace-analysis-patterns-part-52/

# API Layers

![](images/6af20923d4cb9b439b4f01111e1512211cead6f603dd23f9a7ed1b913f097383.jpg)

2023Software Diagnostics Services

In complex applications and services, there can be several API layers. The top layers may reference the lower layers and also the lowest ones for basic functions. The libc layer is usually considered the lowest API level that contains function wrappers for system calls (syscalls). However, any library and executable itself can call syscalls directly as well.

# API Internals

Memory analysis patterns:

Hooked Functions (User Space)   
Module patterns

0 Hooked Modules

Malware analysis patterns:   
Patched Code

GDB Commands

(gdb) disassemble function   
(gdb) x/<n>i address

WinDbg Commands

0:000> !for_each_module

0:000>u fname

0:000> uf /c fname

2023Software Diagnostics Services

Linux API functions can be patched by value-adding code and malware. Also, the caller of API may be affected by the order of loaded shared libraries. The next exercise covers ADDR patterns such as Function Skeleton and Call Path.

# APl and System Calls

Syscall numbers and arguments   
APl that does not require syscalls

strlen   
malloc   
fread

APl that requires kernel services

getpid   
mmap   
read

2023Software Diagnostics Services

Sometimes, we want to investigate the code of an API function to know what other API functions it uses and whether it is translated to a system call. For example, some API functions do not require kernel services, such as the current process and thread ids. Other functions may cache the result from the kernel or the different execution paths without kernel services.

Syscall numbers and arguments

https://syscall.sh/

# Exercise L6

Goal: Explore APl layers and internals of specific APl functions; check whether the selected APl functions use system calls   
ADDR Patterns: Function Skeleton; Call Path   
\LAPI-Dumps\Exercise-L6-GDB.pdf   
\LAPI-Dumps\Exercise-L6-WinDbg.pdf

2023Software Diagnostics Services

Goal: Explore API layers and internals of specific API functions; check whether the selected API functions use system calls.

ADDR Patterns: Function Skeleton; Call Path.

1. Load a core dump core.9 and bash executable from the x64 directory:

```txt
~/LAPI/x64$ gdb -c core.9 -se bash
GNU gdb (Debian 8.2.1-2+b3) 8.2.1
Copyright (C) 2018 Free Software Foundation, Inc.
License GPLv3+: GNU GPL version 3 or later <http://gnu.org/licenses/gpl.html>
This is free software: you are free to change and redistribute it.
There is NO WARRANTY, to the extent permitted by law.
Type "show copying" and "show warranty" for details.
This GDB was configured as "x86_64-linux-gnu".
Type "show configuration" for configuration details.
For bug reporting instructions, please see:
<http://www.gnu.org/software/gdb/bugs/>. 
Find the GDB manual and other documentation resources online at:
<http://www.gnu.org/software/gdb/documentation/>. 
For help, type "help".
Type "apropos word" to search for commands related to "word"... 
Reading symbols from bash...(no debugging symbols found)...done.
warning: core file may not match specified executable file.
[New LWP 9]
Core was generated by `-bash'.
#0 0x00007f3e9f7492d7 in __GI__ waitpid (pid=-1, stat_location=0x7ffc661ad0, options=10) at ../sysdeps/unix/sysv/linux/waitpid.c:30
30 .../sysdeps/unix/sysv/linux/waitpid.c: No such file or directory. 
```

2. Set logging to a file in case of lengthy output from some commands:

```txt
(gdb) set logging on L6.log Copying output to L6.log. 
```

3. Let’s follow the call path of the _nc_is_dir_path function from the libtinfo.so library:

```asm
(gdb) disassemble _nc_is_dir_path
Dump of assembler code for function _nc_is_dir_path:
0x00007f3e9f856b50 <=0>: sub $0xa8,%rsp
0x00007f3e9f856b57 <=7>: mov %rdi,%rsi
0x00007f3e9f856b5a <=10>: mov $0x1,%edi
0x00007f3e9f856b5f <=15>: mov %fs:0x28,%rax
0x00007f3e9f856b68 <=24>: mov %rax,0x98(%rsp)
0x00007f3e9f856b70 <=32>: xor %eax,%eax
0x00007f3e9f856b72 <=34>: mov %rsp,%rdx
0x00007f3e9f856b75 <=37>: callq 0x7f3e9f856550 <_xstat@plt>
0x00007f3e9f856b7a <=42>: xor %edx,%edx
0x00007f3e9f856b7c <=44>: test %eax,%eax
0x00007f3e9f856b7e <=46>: jne 0x7f3e9f856b91 <_nc_is_dir_path+65>
0x00007f3e9f856b80 <=48>: mov 0x18(%rsp),%eax
0x00007f3e9f856b84 <=52>: and $0xf000,%eax
0x00007f3e9f856b89 <=57>: cmp $0x4000,%eax 
```

```txt
0x00007f3e9f856b8e \(< + 62>\) : sete %dl   
0x00007f3e9f856b91 \(< + 65>\) : mov 0x98(%rsp),%rcx   
0x00007f3e9f856b99 \(< + 73>\) : xor %fs:0x28,%rcx   
0x00007f3e9f856ba2 \(< + 82>\) : mov %edx,%eax   
0x00007f3e9f856ba4 \(< + 84>\) : jne 0x7f3e9f856bae <_nc_is_dir_path+94>   
0x00007f3e9f856ba6 \(< + 86>\) : add \$0xa8,%rsp   
0x00007f3e9f856bad \(< + 93>\) : retq   
0x00007f3e9f856bae \(< + 94>\) : callq 0x7f3e9f8562a0 <_stack_chk_fail@plt>   
End of assembler dump.   
(gdb) disassemble 0x7f3e9f856550   
Dump of assembler code for function _xstat@plt:   
0x00007f3e9f856550 \(< + 0>\) : jmpq \*0x1e7aa(%rip) # 0x7f3e9f874d00 <_xstat@got.plt>   
0x00007f3e9f856556 \(< + 6>\) : pushq \$0x52   
0x00007f3e9f85655b \(< + 11>\) : jmpq 0x7f3e9f856020   
End of assembler dump.   
(gdb) x/a 0x7f3e9f874d00   
0x7f3e9f874d00 <_xstat@got.plt>: 0x7f3e9f76c940 <_GI_xstat>   
(gdb) disassemble 0x7f3e9f76c940   
Dump of assembler code for function _GI_xstat:   
0x00007f3e9f76c940 \(< + 0>\) : mov %rsi,%rax   
0x00007f3e9f76c943 \(< + 3>\) : cmp \$0x1,%edi   
0x00007f3e9f76c946 \(< + 6>\) : ja 0x7f3e9f76c978 <_GI_xstat+56>   
0x00007f3e9f76c948 \(< + 8>\) : mov %rax,%rdi   
0x00007f3e9f76c94b \(< + 11>\) : mov %rdx,%rsi   
0x00007f3e9f76c94e \(< + 14>\) : mov \$oX4,%eax   
0x00007f3e9f76c953 \(< + 19>\) : syscall   
0x00007f3e9f76c955 \(< + 21>\) : cmp \$oXFFFFFFFoo,\\(rx   
OxOoOoOoF3e9F76c95b \(< + 27>\) : ja 0x7f3e9f76c96O <_GI_xstat+32>   
OxOoOoOoF3e9F76c95d \(< + 29>\) : retq   
OxOoOoOoF3e9F76c95e \(< + 3\theta >\) : xchg %ax,%ax   
OxOoOoOoF3e9F76c96O \(< + 32>\) : mov  OxDof5Og(%rip),%rdx # Ox7f3e9f83ce7O   
OxOoOoOoF3e9F76c967 \(< + 39>\) : neg %eax   
OxOoOoOoF3e9F76c969 \(< + 41>\) : mov %eax,%fs:(%rdx)   
OxOoOoOoF3e9F76c96c \(< + 44>\) : mov \$oXFFFFFF,\\(eax   
OxOoOoOoF3e9F76c971 \(< + 49>\) : retq   
OxOoOoOoF3e9F76c972 \(< + 5\theta >\) : nopw  OxO(%raX,%raX,1)   
OxOoOoOoF3e9F76c978 \(< + 5\theta >\) : mov OxDof4F1(%rip),%rax # Ox7f3e9f83ce7O   
OxOoOoOoF3e9F76c97f \(< + 63>\) : movl \$oX16,%fs:(%rax)   
OxOoOoOoF3e9F76c986 \(< + 7\theta >\) : mov \$oXFFFFFF,\\(eax   
OxOoOoOoF3e9F76c98b \(< + 75>\) : retq   
End of assembler dump. 
```

Note: We see that the _nc_is_dir_path function from the libtinfo.so library calls the _xstat function from the libc.so library, and the latter uses the syscall number 4 (stat).

4. Load a core dump core.19649 and bash executable from the A64 directory:

```txt
\~/LAPI/x64\$ cd ../A64 
```

```txt
~/LAPI/A64$ gdb-multiarch -c core.19649 -se bash
GNU gdb (Debian 8.2.1-2+b3) 8.2.1
Copyright (C) 2018 Free Software Foundation, Inc.
License GPLv3+: GNU GPL version 3 or later <http://gnu.org/licenses/gpl.html>
This is free software: you are free to change and redistribute it.
There is NO WARRANTY, to the extent permitted by law. 
```

```txt
Type "show copying" and "show warranty" for details.  
This GDB was configured as "x86_64-linux-gnu".  
Type "show configuration" for configuration details.  
For bug reporting instructions, please see: <http://www.gnu.org/software/gdb/bugs/>.  
Find the GDB manual and other documentation resources online at: <http://www.gnu.org/software/gdb/documentation/>. 
```

```txt
For help, type "help". Type "apropos word" to search for commands related to "word"... Reading symbols from bash...(no debugging symbols found)...done. 
```

```yaml
warning: core file may not match specified executable file. [New LWP 19649] 
```

```txt
warning: Could not load shared library symbols for 3 libraries, e.g. /lib/aarch64-linux-gnu/libtinfo.so.6. Use the "info sharedlibrary" command to see the complete listing. Do you need "set solib-search-path" or "set sysroot"? Core was generated by `-bash'. #0 0x0000fffffbafa6734 in ?? () 
```

```txt
(gdb) set solib-search-path .   
Reading symbols from /home/coredump/LAPI/A64/libtinfo.so.6...(no debugging symbols found)...done.   
Reading symbols from /home/coredump/LAPI/A64/libc.so.6...(no debugging symbols found)...done.   
Reading symbols from /home/coredump/LAPI/A64/ld-linux-aarch64.so.1...(no debugging symbols found)...done. 
```

5. Set logging to a file in case of lengthy output from some commands:

```txt
(gdb) set logging on L6.log  
Copying output to L6.log. 
```

6. Disassemble the getpid and getppid functions and find their syscall numbers:

```txt
(gdb) disassemble getpid  
Dump of assembler code for function getpid:  
0x0000ffffbafa8000 <= 0>: nop  
0x0000ffffbafa8004 <= 4>: mov x8, #0xac // #172  
0x0000ffffbafa8008 <= 8>: svc #0x0  
0x0000ffffbafa800c <= 12>: ret  
End of assembler dump. 
```

```txt
(gdb) disassemble getppid  
Dump of assembler code for function getppid:  
0x0000ffffbafa8040 <= 0>: nop  
0x0000ffffbafa8044 <= 4>: mov x8, #0xad // #173  
0x0000ffffbafa8048 <= 8>: svc #0x0  
0x0000ffffbafa804c <= 12>: ret  
End of assembler dump. 
```

# Exercise L6 (WinDbg)

Goal: Explore API layers and internals of specific API functions; check whether the selected API functions use system calls.

ADDR Patterns: Function Skeleton; Call Path.

1. Launch WinDbg and load a core dump core.19649 from the A64 directory:

```txt
Microsoft (R) Windows Debugger Version 10.0.25324.1001 AMD64 Copyright (c) Microsoft Corporation. All rights reserved. 
```

```txt
Loading Dump File [C:\LAPI\A64\core.19649] 64-bit machine not using 64-bit API 
```

```txt
********** Path validation summary ****  
Response Time (ms) Location  
Deferred svr*  
Symbol search path is: svr*  
Executable search path is:  
Generic Unix Version 0 UP Free ARM 64-bit (AArch64)  
System Uptime: not available  
Process Uptime: not available  
*** WARNING: Unable to verify timestamp for libc.so.6  
*** WARNING: Unable to verify timestamp for bash  
libc.so+0xb6734:  
0000ffff`bafa6734 d4000001svc #0 
```

2. Set the symbol path and logging to a file in case of lengthy output from some commands:

```txt
0:000> .sympath+ C:\LAPI\A64   
Symbol search path is: srv\;C:\LAPI\A64   
Expanded Symbol search path is: cache\*;SRV\*\*https://msdl.microsoft.com/download/symbols;c:\\lapi\a64   
********** Path validation summary****************** Response Time (ms) Location Deferred srv\ OK C:\LAPI\A64   
*** WARNING: Unable to verify timestamp for libc.so.6   
*** WARNING: Unable to verify timestamp for bash 
```

```txt
0:000> .reload   
....\*\*\* WARNING: Unable to verify timestamp for libc.so.6   
\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\* 
```

```txt
********** Symbol Loading Error Summary ***  
Module name Error  
bash The system cannot find the file specified  
libc.so The system cannot find the file specified 
```

```txt
You can troubleshoot most symbol related issues by turning on symbol loading diagnostics (!sym noisy) and repeating the command that caused symbols to be loaded.  
You should also verify that your symbol search path (.sympath) is correct. 
```

```batch
0:000> .logopen C:\LAPI\A64\L6-WinDbg.log  
Opened log file 'C:\LAPI\A64\L6-WinDbg.log' 
```

3. Let’s see the function skeleton of the nc_is_dir_path function from the libtinfo_so_6 library and follow the call path of the first branch and link:

```csv
0:000>uf/c libtinfo so 6!nc_is_dir_path
libtinfo so 6!nc_is_dir_path (0000ffff\bb0ae9c0)
		...
		...
		...
		...
		...
		...
		...
		...
		...
		...
		...
		...
		...
		...
		...
		...
		...
		...
		...
		...
		...
		...
		...
		...
		...
		...
		...
		...
	...
	...
	...
	...
	...
	...
	...
	...
	...
	...
	...
	...
	...
	...
	...
	...
	...
	...
	...
	...
	...
	...
	...
	...
	...
	...
	...
	...
	...
	...
	...
	...
	...
	...
	...
	...
	...
	...
	...
	...
	...
	...
	...
.
...
...
...
...
...
...
...
...
...
...
...
...
...
...
...
...
...
...
...
...
...
...
.
...
.
.
.
.
.
.
.
.
.
.
.
.
.
.
.
.
.
.
.
.
.
.
.
.
.
.
.
.
.
.
.
.
.
.
.
.
.
.
.
.
.
.
.
.
.
.
.
.
.
.
.
.
.
.
.
.
.
.
.
.
.
.
.
.
.
.
.
.
.
.
....
.
.
.
.
.
.
?
.
.
.
.
.
.
.
.
.
.
.
.
.
.
;
.
....
.
....
?
....
?
....
?
....
?
....
?
....
?
....
?
....
?
....
?
....
?
....
?
....
?
....
?
....
?
....
?
....
?
....
?
....
?
....
?
....
?
....
?
....
?
....
?
....
?
....
?
....
?
....
?
....
?
>
 ...
..
?
...
...
?
...
!
...
?
...
?
...
?
... 
...
... 
... 
... 
... 
... 
... 
... 
... 
... 
... 
... 
... 
... 
... 
... 
... 
... 
... 
... 
... 
... 
... 
... 
... 
... 
... 
... 
... 
... 
... 
... 
... 
... 
... 
... 
... 
... 
... 
... 
... 
... 
... 
... 
... 
... 
... 
... 
... 
... 
... 
... ...
......
......
......
......
......
......
......
......
......
......
......
......
......
......
......
......
......
......
......
......
......
......
......
......
......
......
......
......
......
......
......
......
......
......
......
......
......
......
......
......
......
......
......
......
......
......
......
......
......
......
...... 
```

```txt
0:000> u 0000ffffbb0ad6d0  
libtinfo.so_6!+0x230:  
0000ffffbb0ad6d0 b0000190 adr p xip0, libtinfo so_6!strfnames+0x200 (0000ffffbb0de000)  
0000ffffbb0ad6d4 f9471611 ldr xip1,[xip0,#0xE28]  
0000ffffbb0ad6d8 9138a210 add xip0,xip0,#0xE28  
0000ffffbb0ad6dc d61f0220 br xip1  
0000ffffbb0ad6e0 b0000190 adr p xip0, libtinfo so_6!strfnames+0x200 (0000ffffbb0de000)  
0000ffffbb0ad6e4 f9471a11 ldr xip1,[xip0,#0xE30]  
0000ffffbb0ad6e8 9138c210 add xip0,xip0,#0xE30  
0000ffffbb0ad6ec d61f0220 br xip1 
```

```txt
0:000> dps 0000ffff\bb0de000+0xE28 L1  
0000ffff\bb0dee28 0000ffff\bafc7030 libc(so!stat 
```

```csv
0:000>uflibc(so!stat   
libc(so!stat:   
0000ffff`bafc7030 aa0103e2 mov x2,x1   
0000ffff`bafc7034 52800003 mov w3,#0   
0000ffff`bafc7038 aa0003e1 mov x1,x0   
0000ffff`bafc703c 12800c60 mov w0,#-0x64   
0000ffff`bafc7040 1400001c b libc(so!fstatat (0000ffff`bafc70b0) Branch   
libc(so!fstatat:   
0000ffff`bafc70b0 93407c00 sxtw xo,wO   
0000ffff`bafc70b4 93407c63 sxtw x3,w3   
0000ffff`bafc70b8 d28009e8 mov x8,#Ox4F   
0000ffff`bafc70bc d4000001 svc #O   
OooOFFFF`bafc7OcO 314Oo41f cmn wO,#1,ls1 #OxC   
OooOFFFF`bafc7Oc4 54OoOoo68 bhi libc(so!fstatat+Ox2O (OoooFFFF`bafc7OdO) Branch   
libc(so!fstatat+Ox18:   
OooOFFFF`bafc7Oc8 528OoOOo mov wO,#O   
OooOFFFF`bafc7Occ d65fO3cO ret 
```

```csv
libc(so)!statat+0x20:
0000ffff`bafc70d0 90000622 adr p x2,libc(so)!sys_siglist+0x1a8 (0000ffff~bb08b000)
0000ffff`bafc70d4 f946e442 ldr x2,[x2,#0xDC8]
0000ffff`bafc70d8 d53bd043 mrs x3,TPIDR_EL0
0000ffff`bafc70dc 4b0003e1 neg w1,w0
0000ffff`bafc70e0 1280000 mov w0,#-1
0000ffff`bafc70e4 b8226861 str w1,[x3,x2]
0000ffff`bafc70e8 d65f03c0 ret 
```

Note: We see that the nc_is_dir_path function from the libtinfo.so library calls the stat function from the libc.so library, and the latter uses the syscall number 0x4F (stat).

4. Disassemble the getpid and getppid functions and find their syscall numbers:

```txt
0:000>ufgetpid   
libc.so!getpid:   
0000ffff`bafa8000 d503201f nop   
0000ffff`bafa8004 d2801588 mov x8,#0xAC   
0000ffff`bafa8008 d4000001svc #0   
0000ffff`bafa800c d65f03c0 ret   
0:000>ufgetppid   
libc.so!getppid:   
0000ffff`bafa8040 d503201f nop   
0000ffff`bafa8044 d28015a8 mov x8,#0xCD   
0000ffff`bafa8048 d4000001svc #o   
0000ffff`bafa804c d65f03c0 ret
```

# API Name Patterns

③ create/open/delete/close   
○ pthread   
③ display/surface/window   
@ alloc/free   
O read/write

GDB Commands

(gdb) info functions pattern

WinDbg Commands

0:000>Xmodule!fpattern

2023Software Diagnostics Services

One way to learn about various available API functions is to search for name patterns, such as all functions that have some substring in their symbol name.

# API Namespaces

Functions required to accomplish a particular task

Example: network communication

2023 Software Diagnostics Services

API Namespace is a group or several groups of functions required to accomplish a particular task. A namespace group usually belongs to one particular shared library. As we mentioned before, malware analysis patterns include the Namespace analysis pattern that views groups of imported functions from several shared libraries as potentially serving some particular malicious (but maybe just some utilitarian) need, for example, network communication or a screen capture that needs to be saved somewhere.

# APl Syntagms/Paradigms

Syntagms syntagmatic analysis   
Paradigms / paradigmatic analysis

![](images/8010910f9a408cfd0276c9810faaac72ab50e902bf5c525c9d8e84a393409ff9.jpg)

2023Software Diagnostics Services

API Namespace is a group or several groups of functions required to accomplish a particular task. A namespace group usually belongs to one particular shared library. As we mentioned before, malware analysis patterns include the Namespace analysis pattern that views groups of imported functions from several shared libraries as potentially serving some particular malicious (but maybe just some utilitarian) need, for example, network communication or a screen capture that needs to be saved somewhere.

# Syntagms

https://en.wikipedia.org/wiki/Syntagma_(linguistics)

# Syntagmatic analysis

https://en.wikipedia.org/wiki/Syntagmatic_analysis

# Paradigmatic analysis

https://en.wikipedia.org/wiki/Paradigmatic_analysis

# Marked API

Marked Message trace and log analysis pattern   
Points to the presence or absence of activity   
Example:

execve   
socket   
connect   
create

GDB Commands

(gdb) info functions @plt

2023Software Diagnostics Services

We call by marked API certain functions imported or present in call paths that point to the presence of activity. If they are not imported and not used, they point to the absence of activity. Such API functions may be compiled into a checklist. This API aspect is borrowed from Marked Message trace and log analysis pattern where marked messages may point to some domain of software activity related to functional requirements and help in troubleshooting and debugging, and some unmarked messages may directly say about the absences of activity.

# Marked Message

https://www.dumpanalysis.org/blog/index.php/2012/01/02/trace-analysis-patterns-part-45/

# ADDR Patterns

From Accelerated Disassembly Deconstruction Reversing   
List of pattern names   
Pattern descriptions

2023Software Diagnostics Services

The name ADDR (sounds like an address) comes from the Accelerated Disassembly Deconstruction Reversing abbreviation from the similar sounding training course. The slide provides the link to their names and the link to their descriptions. We used some ADDR patterns, such as Call Path and Function Skeleton, in some of our exercises.

List of pattern names

https://www.dumpanalysis.org/addr-patterns

Pattern descriptions

https://www.patterndiagnostics.com/Training/Accelerated-Linux-Disassembly-Reconstruction-Reversing-Second-

Edition-Slides.pdf

# DebugWare Patterns

Patterns for troubleshooting and debugging tools   
○ API Query

Periodic or asynchronous query of the same set of APls and logging of their input and output data.

2023Software Diagnostics Services

DebugWare patterns are patterns for designing and implementing troubleshooting and debugging tools. One of the first patterns was named API Query, where some API functions are used periodically to query OS data and log the output.

API Query

https://www.dumpanalysis.org/blog/index.php/2008/07/19/debugware-patterns-part-1/

# Patterns vs. Analysis Patterns

Diagnostic Pattern: a common recurrent identifiable problem together with a set of recommendations and possible solutions to apply in a specific context.

Diagnostic Problem: a set of indicators (symptoms, signs) describing a problem.

Diagnostic Analysis Pattern: a common recurrent analysis technique and method of diagnostic pattern identification in a specific context.

Diagnostics Pattern Language: common names of diagnostic and diagnostic analysis patterns. The same language for any operating system: Windows, macOS, Linux,...

2023 Software Diagnostics Services

We have now come to the association of Linux API with various diagnostic analysis patterns. Here, I’d like to stress the informal difference between patterns and analysis patterns. The distinction is highlighted on this slide.

# Memory Analysis Patterns

# User space

Process memory dumps

# Function analysis patterns

· Stack Trace Collection   
· Well-Tested Function   
· False Function Parameters   
· String Parameter   
· Small Value / Design Value

![](images/3c14f41ce58a3985e16a409a4bc789ce359566af4ddaa8fa175acd3496c34392.jpg)

· Stack Trace   
· Execution Residue   
· Hidden Parameter   
· Parameter Flow   
· Data Correlation

2023Software Diagnostics Services

We pay attention to Linux API function calls when we do diagnostics and postmortem debugging using memory dumps. Since API calls are done in user space, we are interested only in the process and possibly physical memory dumps unless we need to follow syscalls into kernel space. On this slide, I put some general analysis patterns related to function parameters. Execution Residue allows us to see past execution traces of Linux API, such as their return addresses in the stack regions. We mention specific memory analysis patterns associated with particular Linux API categories when we look at the latter.

# Thread and Adjoint Thread

<table><tr><td>PID</td><td>PPID</td><td>USER</td><td>PR</td><td>NI</td><td>VIRT</td><td>RES</td><td>SHR</td><td>S</td><td>%CPU</td><td>%MEM</td></tr><tr><td>22992</td><td>1</td><td>root</td><td>20</td><td>0</td><td>2368</td><td>124</td><td>0</td><td>S</td><td>0.0</td><td>0.0</td></tr><tr><td>23900</td><td>1</td><td>root</td><td>20</td><td>0</td><td>2368</td><td>124</td><td>0</td><td>S</td><td>0.0</td><td>0.0</td></tr><tr><td>23999</td><td>1</td><td>root</td><td>20</td><td>0</td><td>2368</td><td>124</td><td>0</td><td>S</td><td>0.0</td><td>0.0</td></tr><tr><td>23917</td><td>1</td><td>root</td><td>20</td><td>0</td><td>2352</td><td>112</td><td>0</td><td>S</td><td>0.0</td><td>0.0</td></tr><tr><td>23918</td><td>23917</td><td>root</td><td>20</td><td>0</td><td>2368</td><td>124</td><td>0</td><td>S</td><td>0.0</td><td>0.0</td></tr><tr><td>23926</td><td>1</td><td>root</td><td>20</td><td>0</td><td>2352</td><td>112</td><td>0</td><td>S</td><td>0.0</td><td>0.0</td></tr><tr><td>23927</td><td>23926</td><td>root</td><td>20</td><td>0</td><td>2368</td><td>124</td><td>0</td><td>S</td><td>0.0</td><td>0.0</td></tr><tr><td>9</td><td>8</td><td>coredump</td><td>20</td><td>0</td><td>6992</td><td>3676</td><td>3104</td><td>S</td><td>0.0</td><td>0.0</td></tr><tr><td>12</td><td>9</td><td>coredump</td><td>20</td><td>0</td><td>2184</td><td>1556</td><td>1464</td><td>S</td><td>0.0</td><td>0.0</td></tr><tr><td>2177</td><td>2176</td><td>coredump</td><td>20</td><td>0</td><td>2388</td><td>692</td><td>624</td><td>S</td><td>0.0</td><td>0.0</td></tr><tr><td>2178</td><td>2177</td><td>coredump</td><td>20</td><td>0</td><td>2388</td><td>748</td><td>680</td><td>S</td><td>0.0</td><td>0.0</td></tr><tr><td>2183</td><td>2178</td><td>coredump</td><td>20</td><td>0</td><td>2388</td><td>756</td><td>688</td><td>S</td><td>0.0</td><td>0.0</td></tr><tr><td>2187</td><td>2183</td><td>coredump</td><td>20</td><td>0</td><td>946824</td><td>107840</td><td>34864</td><td>S</td><td>0.3</td><td>1.3</td></tr><tr><td>2198</td><td>2187</td><td>coredump</td><td>20</td><td>0</td><td>645928</td><td>62652</td><td>32148</td><td>S</td><td>0.3</td><td>0.8</td></tr><tr><td>2236</td><td>2187</td><td>coredump</td><td>20</td><td>0</td><td>1021300</td><td>160888</td><td>37840</td><td>R</td><td>0.7</td><td>2.0</td></tr><tr><td>2247</td><td>2187</td><td>coredump</td><td>20</td><td>0</td><td>837844</td><td>73376</td><td>31768</td><td>S</td><td>0.3</td><td>0.9</td></tr><tr><td>2347</td><td>2198</td><td>coredump</td><td>20</td><td>0</td><td>7740</td><td>4488</td><td>3196</td><td>S</td><td>0.0</td><td>0.1</td></tr><tr><td>2517</td><td>2516</td><td>coredump</td><td>20</td><td>0</td><td>320880</td><td>34576</td><td>26260</td><td>S</td><td>0.0</td><td>0.4</td></tr><tr><td>2526</td><td>2525</td><td>coredump</td><td>20</td><td>0</td><td>320880</td><td>32472</td><td>26196</td><td>S</td><td>0.0</td><td>0.4</td></tr><tr><td>4059</td><td>2236</td><td>coredump</td><td>20</td><td>0</td><td>1867316</td><td>823340</td><td>14868</td><td>S</td><td>0.3</td><td>10.2</td></tr><tr><td>8072</td><td>2236</td><td>coredump</td><td>20</td><td>0</td><td>591712</td><td>49780</td><td>31664</td><td>S</td><td>0.0</td><td>0.6</td></tr><tr><td>8298</td><td>8297</td><td>coredump</td><td>20</td><td>0</td><td>7636</td><td>4480</td><td>3284</td><td>S</td><td>0.0</td><td>0.1</td></tr><tr><td>21401</td><td>21400</td><td>coredump</td><td>20</td><td>0</td><td>320880</td><td>34680</td><td>26364</td><td>S</td><td>0.0</td><td>0.4</td></tr><tr><td>21645</td><td>21644</td><td>coredump</td><td>20</td><td>0</td><td>320880</td><td>34660</td><td>26340</td><td>S</td><td>0.0</td><td>0.4</td></tr><tr><td>21654</td><td>21653</td><td>coredump</td><td>20</td><td>0</td><td>320880</td><td>32484</td><td>26204</td><td>S</td><td>0.0</td><td>0.4</td></tr><tr><td>21930</td><td>21929</td><td>coredump</td><td>20</td><td>0</td><td>320880</td><td>325804</td><td>26228</td><td>S</td><td>0.0</td><td>0.4</td></tr><tr><td>22568</td><td>8298</td><td>coredump</td><td>20</td><td>0</td><td>23816</td><td>9176</td><td>6996</td><td>S</td><td>0.0</td><td>0.1</td></tr><tr><td>22570</td><td>22568</td><td>coredump</td><td>20</td><td>0</td><td>7124</td><td>3964</td><td>3224</td><td>S</td><td>0.0</td><td>0.0</td></tr><tr><td>22942</td><td>22941</td><td>coredump</td><td>20</td><td>0</td><td>320880</td><td>34576</td><td>26260</td><td>S</td><td>0.0</td><td>0.4</td></tr><tr><td>22951</td><td>22950</td><td>coredump</td><td>20</td><td>0</td><td>320880</td><td>32588</td><td>26388</td><td>S</td><td>0.0</td><td>0.4</td></tr><tr><td>22984</td><td>22983</td><td>coredump</td><td>20</td><td>0</td><td>320880</td><td>32584</td><td>26228</td><td>S</td><td>0.0</td><td>0.4</td></tr><tr><td>22993</td><td>22992</td><td>coredump</td><td>20</td><td>0</td><td>320880</td><td>34544</td><td>26228</td><td>S</td><td>0.0</td><td>0.4</td></tr><tr><td>23901</td><td>23900</td><td>coredump</td><td>20</td><td>0</td><td>320880</td><td>32616</td><td>26340</td><td>S</td><td>0.0</td><td>0.4</td></tr><tr><td>23910</td><td>23909</td><td>coredump</td><td>20</td><td>0</td><td>320880</td><td>34552</td><td>26232</td><td>S</td><td>0.0</td><td>0.4</td></tr><tr><td>23919</td><td>23918</td><td>coredump</td><td>20</td><td>0</td><td>587176</td><td>43444</td><td>29976</td><td>S</td><td>0.0</td><td>0.5</td></tr><tr><td>23928</td><td>23927</td><td>coredump</td><td>20</td><td>0</td><td>588688</td><td>43998</td><td>29716</td><td>S</td><td>0.0</td><td>0.5</td></tr><tr><td>24407</td><td>2176</td><td>coredump</td><td>20</td><td>0</td><td>5250736</td><td>24184</td><td>10204</td><td>S</td><td>0.0</td><td>0.3</td></tr><tr><td>25806</td><td>22570</td><td>coredump</td><td>20</td><td>0</td><td>10980</td><td>3440</td><td>2976</td><td>R</td><td>0.3</td><td>0.0</td></tr></table>

<table><tr><td>PID</td><td>PPID</td><td>USER</td><td>PR</td><td>NI</td><td>VIRT</td><td>RES</td><td>SHR</td><td>S</td><td>%CPU</td><td>%MEM</td></tr><tr><td>8298</td><td>8297</td><td>coredump</td><td>20</td><td>0</td><td>7636</td><td>4480</td><td>3284</td><td>S</td><td>0.0</td><td>0.1</td></tr><tr><td>8297</td><td>8296</td><td>root</td><td>20</td><td>0</td><td>2348</td><td>120</td><td>0</td><td>S</td><td>0.0</td><td>0.0</td></tr><tr><td>2526</td><td>2525</td><td>coredump</td><td>20</td><td>0</td><td>320880</td><td>32472</td><td>26196</td><td>S</td><td>0.0</td><td>0.4</td></tr><tr><td>2517</td><td>2516</td><td>coredump</td><td>20</td><td>0</td><td>320880</td><td>34576</td><td>26260</td><td>S</td><td>0.0</td><td>0.4</td></tr><tr><td>4059</td><td>2236</td><td>coredump</td><td>20</td><td>0</td><td>1867316</td><td>823340</td><td>14868</td><td>S</td><td>0.3</td><td>10.2</td></tr><tr><td>8072</td><td>2236</td><td>coredump</td><td>20</td><td>0</td><td>591712</td><td>49780</td><td>31664</td><td>S</td><td>0.0</td><td>0.6</td></tr><tr><td>2347</td><td>2198</td><td>coredump</td><td>20</td><td>0</td><td>7740</td><td>4488</td><td>3196</td><td>S</td><td>0.0</td><td>0.1</td></tr><tr><td>2198</td><td>2187</td><td>coredump</td><td>20</td><td>0</td><td>645928</td><td>61892</td><td>32148</td><td>S</td><td>0.0</td><td>0.8</td></tr><tr><td>2236</td><td>2187</td><td>coredump</td><td>20</td><td>0</td><td>1021300</td><td>161120</td><td>37040</td><td>S</td><td>0.3</td><td>2.0</td></tr><tr><td>2247</td><td>2187</td><td>coredump</td><td>20</td><td>0</td><td>837844</td><td>73672</td><td>31768</td><td>S</td><td>0.0</td><td>0.9</td></tr><tr><td>2187</td><td>2183</td><td>coredump</td><td>20</td><td>0</td><td>946236</td><td>108696</td><td>34864</td><td>S</td><td>0.0</td><td>1.3</td></tr><tr><td>2183</td><td>2178</td><td>coredump</td><td>20</td><td>0</td><td>2388</td><td>756</td><td>688</td><td>S</td><td>0.0</td><td>0.0</td></tr><tr><td>2178</td><td>2177</td><td>coredump</td><td>20</td><td>0</td><td>2388</td><td>748</td><td>680</td><td>S</td><td>0.0</td><td>0.0</td></tr><tr><td>2177</td><td>2176</td><td>coredump</td><td>20</td><td>0</td><td>2388</td><td>692</td><td>624</td><td>S</td><td>0.0</td><td>0.0</td></tr><tr><td>24407</td><td>2176</td><td>coredump</td><td>20</td><td>0</td><td>5250736</td><td>24184</td><td>10204</td><td>S</td><td>0.0</td><td>0.3</td></tr><tr><td>2176</td><td>2175</td><td>root</td><td>20</td><td>0</td><td>2348</td><td>120</td><td>0</td><td>S</td><td>0.0</td><td>0.0</td></tr><tr><td>12</td><td>9</td><td>coredump</td><td>20</td><td>0</td><td>2184</td><td>1556</td><td>1464</td><td>S</td><td>0.0</td><td>0.0</td></tr><tr><td>9</td><td>8</td><td>coredump</td><td>20</td><td>0</td><td>6992</td><td>3676</td><td>3104</td><td>S</td><td>0.0</td><td>0.0</td></tr><tr><td>8</td><td>7</td><td>root</td><td>20</td><td>0</td><td>2348</td><td>120</td><td>0</td><td>S</td><td>0.0</td><td>0.0</td></tr><tr><td>4</td><td>1</td><td>root</td><td>20</td><td>0</td><td>2384</td><td>108</td><td>68</td><td>S</td><td>0.0</td><td>0.0</td></tr><tr><td>7</td><td>1</td><td>root</td><td>20</td><td>0</td><td>2332</td><td>112</td><td>0</td><td>S</td><td>0.0</td><td>0.0</td></tr><tr><td>2175</td><td>1</td><td>root</td><td>20</td><td>0</td><td>2332</td><td>112</td><td>0</td><td>S</td><td>0.0</td><td>0.0</td></tr><tr><td>2516</td><td>1</td><td>root</td><td>20</td><td>0</td><td>2368</td><td>124</td><td>0</td><td>S</td><td>0.0</td><td>0.0</td></tr><tr><td>2525</td><td>1</td><td>root</td><td>20</td><td>0</td><td>2368</td><td>124</td><td>0</td><td>S</td><td>0.0</td><td>0.0</td></tr><tr><td>8296</td><td>1</td><td>root</td><td>20</td><td>0</td><td>2332</td><td>112</td><td>0</td><td>S</td><td>0.0</td><td>0.0</td></tr><tr><td>21400</td><td>1</td><td>root</td><td>20</td><td>0</td><td>2368</td><td>124</td><td>0</td><td>S</td><td>0.0</td><td>0.0</td></tr><tr><td>21644</td><td>1</td><td>root</td><td>20</td><td>0</td><td>2368</td><td>124</td><td>0</td><td>S</td><td>0.0</td><td>0.0</td></tr><tr><td>21653</td><td>1</td><td>root</td><td>20</td><td>0</td><td>2368</td><td>124</td><td>0</td><td>S</td><td>0.0</td><td>0.0</td></tr><tr><td>21929</td><td>1</td><td>root</td><td>20</td><td>0</td><td>2368</td><td>124</td><td>0</td><td>S</td><td>0.0</td><td>0.0</td></tr><tr><td>22941</td><td>1</td><td>root</td><td>20</td><td>0</td><td>2368</td><td>124</td><td>0</td><td>S</td><td>0.0</td><td>0.0</td></tr><tr><td>22950</td><td>1</td><td>root</td><td>20</td><td>0</td><td>2368</td><td>124</td><td>0</td><td>S</td><td>0.0</td><td>0.0</td></tr><tr><td>22983</td><td>1</td><td>root</td><td>20</td><td>0</td><td>2368</td><td>124</td><td>0</td><td>S</td><td>0.0</td><td>0.0</td></tr><tr><td>22992</td><td>1</td><td>root</td><td>20</td><td>0</td><td>2368</td><td>124</td><td>0</td><td>S</td><td>0.0</td><td>0.0</td></tr><tr><td>23900</td><td>1</td><td>root</td><td>20</td><td>0</td><td>2368</td><td>124</td><td>0</td><td>S</td><td>0.0</td><td>0.0</td></tr><tr><td>23909</td><td>1</td><td>root</td><td>20</td><td>0</td><td>2368</td><td>124</td><td>0</td><td>S</td><td>0.0</td><td>0.0</td></tr><tr><td>23917</td><td>1</td><td>root</td><td>20</td><td>0</td><td>2352</td><td>112</td><td>0</td><td>S</td><td>0.0</td><td>0.0</td></tr><tr><td>23926</td><td>1</td><td>root</td><td>20</td><td>0</td><td>2352</td><td>112</td><td>0</td><td>S</td><td>0.0</td><td>0.0</td></tr><tr><td>1</td><td>0</td><td>root</td><td>20</td><td>0</td><td>2324</td><td>1520</td><td>1408</td><td>S</td><td>0.0</td><td>0.0</td></tr></table>

2023Software Diagnostics Services

Before we look at another type of execution history artifacts, trace and log analysis patterns, I illustrate, using the top command, the notion of Adjoint Thread that figures a lot in trace and log analysis pattern descriptions. In Thread (considered abstractly as some sequential activity, here it is USER), we have the same value in its attribute column, and other column values vary. In Adjoint Thread, we have the same value for another column, for example, PPID, but the values of other columns, including USER, vary.

# Trace and Log Analysis Patterns

# Function calls:

Thread of Activity   
·Fiber of Activity   
Adjoint Thread of Activity   
Strand of Activity   
Discontinuity   
·Fiber Bundle   
· Weave of Activity

![](images/da903d1ea743882ad5a579ec4eee41fa883a0cd16a12b43d9b95f3c78ab16c23.jpg)

2023Software Diagnostics Services

On this slide, I put references to trace and log analysis patterns related to threads, adjoint threads, fiber bundles, and their combinations. For example, discontinuity in messages may show the possible hang when an API function is called.

Thread of Activity

https://www.dumpanalysis.org/blog/index.php/2009/08/03/trace-analysis-patterns-part-7/

Fiber of Activity

https://www.dumpanalysis.org/blog/index.php/2016/06/29/trace-analysis-patterns-part-126/

Adjoint Thread of Activity

https://www.dumpanalysis.org/blog/index.php/2010/03/04/trace-analysis-patterns-part-17/

# Strand of Activity

https://www.dumpanalysis.org/blog/index.php/2020/09/12/trace-analysis-patterns-part-198/

# Discontinuity

https://www.dumpanalysis.org/blog/index.php/2009/08/04/trace-analysis-patterns-part-8/

# Fiber Bundle

https://www.dumpanalysis.org/blog/index.php/2012/09/26/trace-analysis-patterns-part-52/

# Weave of Activity

https://www.dumpanalysis.org/blog/index.php/2020/09/19/trace-analysis-patterns-part-201/

# APl and Errors

errno.h (values)  
errno   
rtld_errno   
GDB needs an executable linked with -pthread   
FS (x64) and TPIDR_EL0 (ARM64) TLS ABI

2023 Software Diagnostics Services

Linux API calls may return errors. These are well-documented for individual API functions. However, we may be interested in the overall collection of error values and their meanings since we may find errors in raw stack data (for example, Execution Residue memory analysis pattern) and also across different threads. Usually, functions related to syscalls set errno, which has thread-local storage, and to access it from GDB, you need the project linked with the pthread library. Minimal loader libc implementation uses single-thread rtld_errno.

# Values

https://learn.microsoft.com/en-us/cpp/c-runtime-library/errno-constants?view=msvc-170

# TLS ABI

https://fuchsia.dev/fuchsia-src/development/kernel/threads/tls

# Exercise L7

Goal: Explore error handling implementation in Linux APl   
ADDR Patterns: Call Epilogue   
Memory Analysis Patterns: Last Error Collection   
\LAPl-Dumps\Exercise-L7-GDB.pdf   
\LAPl-Dumps\Exercise-L7-WinDbg.pdf

2023Software Diagnostics Services

Goal: Explore error handling implementation in Linux API.

ADDR Patterns: Call Epilogue.

Memory Analysis Patterns: Last Error Collection.

1. Load a core dump core.25399 and error executable from the x64 directory:

```txt
~/LAPI/x64$ gdb -c core.25399 -se error
GNU gdb (Debian 8.2.1-2+b3) 8.2.1
Copyright (C) 2018 Free Software Foundation, Inc.
License GPLv3+: GNU GPL version 3 or later <http://gnu.org/licenses/gpl.html>
This is free software: you are free to change and redistribute it.
There is NO WARRANTY, to the extent permitted by law.
Type "show copying" and "show warranty" for details.
This GDB was configured as "x86_64-linux-gnu".
Type "show configuration" for configuration details.
For bug reporting instructions, please see:
<http://www.gnu.org/software/gdb/bugs/>. 
Find the GDB manual and other documentation resources online at:
<http://www.gnu.org/software/gdb/documentation/>. 
For help, type "help".
Type "apropos word" to search for commands related to "word"... 
Reading symbols from error...(no debugging symbols found)...done.
[New LWP 25399]
[New LWP 25401]
[Thread debugging using libthread_db enabled]
Using host libthread_db library "/lib/x86_64-linux-gnu/libthread_db.so.1".
Core was generated by `./error'.
#0 0x00007fa7baba75c0 in __GI__nanosleep (requested_time=requested_time@entry=0x7fffdb5d98e0,
remaining=remaining@entry=0x7fffdb5d98e0)
at ../sysdeps/unix/sysv/linux/nanosleep.c:28
28 ../sysdeps/unix/sysv/linux/nanosleep.c: No such file or directory.
[Current thread is 1 (Thread 0x7fa7baade740 (LWP 25399))] 
```

2. Set logging to a file in case of lengthy output from some commands:

```txt
(gdb) set logging on L7.log Copying output to L7.log. 
```

3. Check the errno value for the current thread:

```txt
(gdb) info variables errno  
All variables matching regular expression "errno": 
```

```txt
File errno.c:  
31: int errno;  
27: int rtld_errno; 
```

```txt
File herrno.c:  
27: int __h_errno; 
```

```txt
(gdb) p errno
$1 = 2 
```

# 4. Check the errno variable from the other thread:

```txt
(gdb) info threads
Id     Target Id   Frame
* 1   Thread 0x7fa7baade740 (LWP 25399) 0x00007fa7baba75c0 in __GI__nanosleep
(requested_time=requested_time@entry=0x7fffdb5d98e0,
remaining=remaining@entry=0x7fffdb5d98e0) at ../sysdeps/unix/sysv/linux/nanosleep.c:28
2   Thread 0x7fa7baadd700 (LWP 25401) 0x00007fa7baba75c0 in __GI__nanosleep
(requested_time=requested_time@entry=0x7fa7baadcea0,
remaining=remaining@entry=0x7fa7baadcea0) at ../sysdeps/unix/sysv/linux/nanosleep.c:28
(gdb) thread 2
[Switching to thread 2 (Thread 0x7fa7baadd700 (LWP 25401))]
#0   0x00007fa7baba75c0 in __GI__nanosleep (requested_time=requested_time@entry=0x7fa7baadcea0,
remaining=remaining@entry=0x7fa7baadcea0)
at ../sysdeps/unix/sysv/linux/nanosleep.c:28
28    in ../sysdeps/unix/sysv/linux/nanosleep.c
(gdb) p errno
$2 = 0 
```

# 5. Check how the variable errno is set in the execve libc library call:

```asm
(gdb) disassemble execve
Dump of assembler code for function execve:
0x00007fa7baba78a0 <=0>: mov $0x3b,%eax
0x00007fa7baba78a5 <=5>: syscall
0x00007fa7baba78a7 <=7>: cmp $0xFFFFFFccccff001,%rax
0x00007fa7baba78ad <=13>: jae 0x7fa7baba78b0 <execve+16>
0x00007fa7baba78af <=15>: retq
0x00007fa7baba78b0 <=16>: mov 0xf35b9(%rip),%rcx # 0x7fa7bac9ae70
0x00007fa7baba78b7 <=23>: neg %eax
0x00007fa7baba78b9 <=25>: mov %eax,%fs:(%rcx)
0x00007fa7baba78bc <=28>: or $0xFFFFFFccccff,%,rax
0x00007fa7baba78c0 <=32>: retq
End of assembler dump. 
```

# 6. We can also check all per-thread errno values at once:

(gdb) thread apply all p errno   
Thread 2 (Thread 0x7fa7baadd700 (LWP 25401)): \\(1 = 0   
Thread 1 (Thread 0x7fa7baade740 (LWP 25399)): \\)2 = 2

Goal: Explore error handling implementation in Linux API.

ADDR Patterns: Call Epilogue.

1. Launch WinDbg and load a core dump core.105090 from the A64 directory:

```txt
Microsoft (R) Windows Debugger Version 10.0.25324.1001 AMD64 Copyright (c) Microsoft Corporation. All rights reserved. 
```

```batch
Loading Dump File [C:\LAPI\A64\core.105090] 64-bit machine not using 64-bit API 
```

```txt
********** Path validation summary ****  
Response Time (ms) Location  
Deferred svr*  
Symbol search path is: svr*  
Executable search path is:  
Generic Unix Version 0 UP Free ARM 64-bit (AArch64)  
System Uptime: not available  
Process Uptime: not available  
*** WARNING: Unable to verify timestamp for libc.so.6  
libc.so+0xb1924:  
0000ffffb8ae1924 d4000001svc #0 
```

2. Set the symbol path and logging to a file in case of lengthy output from some commands:

```txt
0:000> .sympath+ C:\LAPI\A64   
Symbol search path is:srv\;C:\LAPI\A64   
Expanded Symbol search path is:   
cache\*;SRV\*\*https://msdl.microsoft.com/download/symbols;c:\\lapi\a64 
```

```txt
********** Path validation summary**********  
Response Time (ms) Location  
Deferred srv* OK C:\LAPI\A64  
*** WARNING: Unable to verify timestamp for libc.so.6 
```

```txt
0:000> .reload  
...*** WARNING: Unable to verify timestamp for libc.so.6 
```

```txt
********** Symbol Loading Error Summary ***  
Module name Error  
libc.so The system cannot find the file specified 
```

```txt
You can troubleshoot most symbol related issues by turning on symbol loading diagnostics (!sym noisy) and repeating the command that caused symbols to be loaded. You should also verify that your symbol search path (.sympath) is correct. 
```

```batch
0:000> .logopen C:\LAPI\A64\L7-WinDbg.log  
Opened log file 'C:\LAPI\A64\L7-WinDbg.log' 
```

```txt
0:000> ~\*k 
```

```txt
Unable to get thread data for thread 0. 0 Id: 19a82.19a82 Suspend: 0 Teb: 00000000`00000000 Unfrozen # Child-SP RetAddr Call Site 00 0000ffff`e57823a0 0000ffff`b8ae6aec libc.so!clock_nanosleep+0x104 01 0000ffff`e5782420 0000ffff`b8ae69b8 libc.so!nanosleep+0x1c 02 0000ffff`e5782430 0000aaaa`d9be0950 libc.so!sleep+0x48 03 0000ffff`e5782480 0000ffff`b8a573fc error!main+0x5c 04 0000ffff`e57824b0 0000ffff`b8a574cc libc.so!_libc_init_first+0x7c 05 0000ffff`e57825c0 0000aaaa`d9be07f0 libc.so!_libc_start_main+0x98 06 0000ffff`e5782620 fFFFFFF`FFFFFF'error!start+Ox3o 07 0000ffff`e5782620 0000000`0000000 0xxxxxx fFFFFFF' Unfrozen #Child-SP RetAddr Call Site 0o 0ooofff`b8a2e74o 0ooofff`b8ae6aec libc.so!clock_nanosleep+Ox1o4 01 0ooofff`b8a2e7cO 0ooofff`b8ae69b8 libc.so!nanosleep+Ox1c 02 0ooofff`b8a2e7dO 0oooaaa`d9beO8e8 libc.so!sleep+Ox48 03 0ooofff`b8a2e82O 0ooofff`b8aad5c8 error!thread_func+Ox14 04 0ooofff`b8a2e84O 0ooofff`b8b15d1c libc.so!pthead_condartr_setpshared+Ox4f8 05 0ooofff`b8a2e96O fFFFFFF`FFFFFF' libc.so!clone+Ox5c 06 0ooofff`b8a2e96O 0ooooooo`ooooooo Oxxxxxxxx 
```

4. Check how the variable errno is set in the execve libc library call:

```csv
0:000>uf execve   
libc(so!_libc_start_main+0x15c:   
0000ffffb8a57590 90000ba2adrp x2,libc.so!sys_siglist+0x1a8 (0000ffffb8bcb000)   
0000ffffb8a57594 f946e442ldr x2,[x2,#0xC8]   
0000ffffb8a57598 d53bd043 mrs x3,TPIDR_EL0   
0000ffffb8a5759c aa0003e1 mov x1,xo   
0000ffffb8a575a0 9280000 mov xo,#-1   
0000ffffb8a575a4 4b0103e1 neg w1,w1   
0000ffffb8a575a8 b8226861 str w1,[x3,x2]   
0000ffffb8a575ac d65f03c0 ret   
libc.so!execve:   
0000ffffb8ae74c0 d503201f nop   
0000ffffb8ae74c4 d2801ba8 mov x8,#OxCD   
0000ffffb8ae74c8 d4000001svc #o   
0000ffffb8ae74cc b13ffc1cmn xo,#OxFF   
0000ffffb8ae74d0 54000042 bhs libc.so!execve+Ox18 (OooFFFFb8ae74d8) Branch   
libc.so!execve+Ox14:   
0000ffffb8ae74d4 d65fO3cO ret Branch   
libc.so!execve+Ox18:   
Ooooffffb8ae74d8 17fdcO2e b libc.so!_libc_start_main+Ox15c (OoooFFFFb8a5759O) Branch 
```

# APl and Functional Programming

Referential transparency   
strlen

strlen(s)，strlen(s) → n,n

read

read(fd， buf，len),

read(fd，buf，len) → n1，n2

Side effects

2023Software Diagnostics Services

With the rise of functional programming during the last decade and due to my own experience with FP, I put this new that shows the relation of API to referential transparency and side effects. In summary, referential transparency allows the substitution of expressions with their value. This requires that the result of the expression is the same for the same input values and that there are no side effects, modifications of memory, or I/O outside of the expression. For example, the length of the string is the same for the same string, and no memory is modified outside. In contrast, the read function not only may have a different output for the same parameter values but also modifies memory outside with unknown values every time it is called.

# Referential transparency

https://en.wikipedia.org/wiki/Referential_transparency

# Side effects

https://en.wikipedia.org/wiki/Side_effect_(computer_science)

# APl and Security

Maliciousness: What, When, Where

SecQuant: Quantifying Container System Call Exposure   
System call risk level classification /syscalls/masks

Vulnerability: How

SAST   
Static code analysis tools

2023Software Diagnostics Services

It is difficult to put all security issues with API on just one slide. So, I attempted to make a view of the surface from a distant star. There are two main aspects: maliciousness of API usage, for example, syscalls, and how the API is used to make it vulnerable, for example, runtime API libraries.

SecQuant: Quantifying Container System Call Exposure

http://www.cs.toronto.edu/~sahil/suneja-esorics22.pdf

System call risk level classification

https://wims.univ-cotedazur.fr/sysmask/doc/technical_txt/risklevels.txt

syscalls

https://wims.univ-cotedazur.fr/sysmask/doc/syscalls-bynumber.html

masks

https://wims.univ-cotedazur.fr/sysmask/doc/masks.txt

SAST

https://en.wikipedia.org/wiki/Static_application_security_testing

Static code analysis tools

https://en.wikipedia.org/wiki/List_of_tools_for_static_code_analysis

# APl and Versioning

Windows: Ex-suffix, longer descriptive function names

CopyFile/CopyFileEx CreateThread/CreateRemoteThread

Linux: numbering, shorter prefixes/suffixes

openat/openat2 accept/accept4 read/pread/readv/preadv/preadv2

2023Software Diagnostics Services

A few words about API versioning in case of adding extensions. In Windows, this is often done via the Ex-suffix or by creating more longer descriptive names. In Linux, it is often done by adding a number or short prefixes and suffixes.

# Linux API Formalization

# Linux APl Formalization

# Ideas from Conceptual Mathematics

2023 Software Diagnostics Services

Now we have a break. We look at possible API formalization using ideas from conceptual mathematics, such as category theory. The exposition is informal. If you already know category theory, you find another application or even provide objections to the approach if you see it wrong. However, if you don’t know category theory, you learn its basic notions and terminology and perhaps apply it to your own areas of interest.

# API Compositionality

# ○Principle of compositionality

![](images/704a193dc8022dd90a8deec3e8e8661adf31cc1a37f9f241b5707cc607ad6d2c.jpg)

2023Software Diagnostics Services

The main principle that allows us to use category theory that I introduce next is the so-called principle of compositionality. We can compose various API calls together via code glue. There’s also possible to connect two API calls by some code, including other API calls if necessary. Here we represent API calls as some objects and code glue as directed arrows.

Principle of compositionality

https://en.wikipedia.org/wiki/Principle_of_compositionality

# Category Theory Language

Category

Objects   
Arrows between objects (must be transitive, if A → B and B → C then A → C)

Functor

Arrow between categories (can be the same category)   
Maps objects to objects and arrows to arrows

Natural Transformation   
Arrows between functors in a category of functors   
Adjunction   
Relationship between functors, change of perspective, back translation

2023Software Diagnostics Services

Category theory allows us to relate different areas analogically via their common structure and behavior. It was introduced in mathematics in the middle of the $2 0 ^ { \mathrm { t h } }$ century. And it has its own language. The three main concepts are categories themself, functors, and natural transformations. To this, I add the so-called adjunction. The definitions I give here are all informal but suitable for our application to API. A category consists of objects and arrows between objects. However, arrows, if they exist between objects, must be composable. There are other restrictions and axioms that I omit for our informal purposes. Categories themselves can be considered objects. Arrows between them are called functors. Functors map objects to objects and arrows to arrows between source and target categories. Functors can be considered as objects themselves in a category of functors. Arrows between them are called natural transformations. The 4th concept, adjunction, is a pair of functors, called left and right adjoints, that are in a special relationship to each other that allows changing of perspective when traveling back and forth between categories. We now make all these 4 concepts visual on the next slide.

# A View of Category Theory

![](images/1b9ab9d7ae45ad00022dd764f44ff610d404d2f2739ef3ffc225a0098a014838.jpg)

2023Software Diagnostics Services

This slide is a visual overview of the four main concepts: category, functor between categories, natural transformation between functors, and adjoint functors. Please notice a kind of tunnel functor arrows that highlight back translation between objects of categories.

# Category Theory Square

![](images/abc5c03e9cf3d62a585a165921079fecc2cc77c5fc010696ca7000673fa77b25.jpg)

2023Software Diagnostics Services

We can arrange all four concepts via the category theory square that shows their relationship that is easy to remember.

# API Category

APl as objects, glue code as arrows   
APl as arrows, glue code as objects fails at composition   
Initial and terminal APl objects in subcategories

![](images/7ef887f2576d6e295a7d3451a4d9c951f70533df272d3f85453f99cd04789904.jpg)

2023Software Diagnostics Services

Let’s translate all those informal abstract concepts into the API world. Again, in the API category, API functions are objects, and glue code are arrows. The other possible dual arrangement, API as arrows, is invalid due to impossible compositions: we cannot get API by composing two API calls. The result of the composition is an object, glue code. Some API functions can be initial or terminal in small subcategories, for example, open and close.

# API Functor

Translates between API layers (different APl)   
Stack trace as functor   
Translates between different APl sequences   
Endofunctor - between the same APl   
Translates between diferent code implementations

@ 2023 Software Diagnostics Services

Different API layers could be different API categories or subcategories of the same category. All such translations are functors. It also suggests interpreting stack traces as functors. Finally, translation between different API sequences is also a functor that maps API calls to API calls and glue code to glue code.

# API Diagram

Indexed set →→ diagram   
Functor from a shape (pattern)

![](images/a0df1b02584713218b25949e8ac92231084f1e107172a7f558477d2cb1fb01fb.jpg)

![](images/6498549c0014f86b11c9aa292f95fa28b9e0b251239511b3b96310c82b3d0212.jpg)

![](images/748300efc086729711a71af07e25f4981e5df61c6daffd461365c01a13f76e59.jpg)

2023Software Diagnostics Services

You may have noticed that category theory illustrations are similar to graphs and diagrams. In category theory, a diagram in some category C is a functor $D$ from some shape category where we don’t care about individual objects and arrows to that category C. They are categorical analogs to indexed sets where a shape is a set of indexes.

# APl Natural Transformation

O Maps between different vertical APl sequences (stack traces)   
Maps between different code translations   
Diagnostics and debugging as natural transformation

![](images/ed116e019810225474f87a5004449c2912719e60b5b081b2b7befbac0a86c675.jpg)

2023Software Diagnostics Services

If stack traces are functors, then maps between them are natural transformations. The same goes for maps between different code translations. Hence, it naturally suggests a categorical interpretation of diagnostics and debugging as natural transformation.

# Cross-platform APl

Windows API / Linux API   
Similar diagrams   
Cross-platform development as a natural transformation

2023Software Diagnostics Services

Diagrams are functors. Therefore, maps between diagrams are natural transformations. We can represent various API sequences in different operating systems as similar diagrams, and this enables us to view cross-platform development as a natural transformation.

# API Adjunction

Navigation between different APl sequences   
Call and return stack trace sequences, callbacks (when stack traces correspond to vertical APl sequences)   
O Back translation between traces/logs (when traces correspond to APl horizontal sequences)

![](images/3110c1fbffbc718d977dc8f60aae2c1fceada107c8141aaad95b712b66ac0305.jpg)

![](images/cd0852e6327f30d54d68b98826bd702f6a01f3cf6afccd2d7bfcada452960f30.jpg)

2023Software Diagnostics Services

API adjunction formalizes back-and-forth navigation between different API sequences, be it traces and logs (horizontal API sequences) or call and return stack trace sequences (vertical API sequences). In some way, it corresponds to changes in perspective, especially when navigating around logs.

# Informal n-APl

Arrows between arrows   
1-APl - normal APl usage   
2-APl - diagnostics, debugging   
3-APl- higher diagnostics,debugging (debugging the debugging)   
∞-APl - for homework ③

2023Software Diagnostics Services

On this slide, we suggest the further informal application of n-category theory, where we consider arrows between arrows. 1-category here maps to normal API usage. 2-category corresponds to diagnostics and debugging. 3-category corresponds to higher level diagnostics and debugging, diagnosing diagnostics and debugging, debugging diagnostics and debugging.

# APl and Trace Categories

1-category APl (semigroup

![](images/208f7cd69e20773542581b311c07bdfcf1d0fce92793c9759a048d2ad5f7bd5d.jpg)

2-category of traces and logs

![](images/99f267b69518ce9758746150d62afeeedf2b638f4197f90daba8903e587a15d1.jpg)

![](images/50c9e1160f0dd8fb90d0e0f2e6a44376a8d772a3812b9cab2de6d80a6708d80d.jpg)

2023Software Diagnostics Services

There can be various categories formed from API and code. We have already looked at one where the API calls are objects connected via code arrows. There can be a different view and a different category of one object, Code, and API calls as arrows. Arrows between arrows may correspond to traces and logs, and these form the 2-category. The original motivation for this view comes from this link:

2-category of traces and logs

https://www.dumpanalysis.org/traces-logs-as-2-categories

semigroup

https://en.wikipedia.org/wiki/Semigroup

# API I/O

Categories - one input, one output   
Operads - many inputs, one output   
Properads - many inputs, many outputs

```txt
int socketpair( int domain, int type, int protocol, int sv[2] ); 
```

![](images/678a88c41300f409f78f08bf8f732e8f3028eb7d549d090c0f53acc04a23c615.jpg)

2023 Software Diagnostics Services

Finally, we consider API functions in terms of their input/output. Since objects of categories have one input and one output, we abstract from API input and consider it as some glue code. In functional programming languages, there may be many inputs, and one produced output, and these mathematical objects (operations) are called operads. In Linux API, outputs may be modified, producing new values, and therefore, we have several inputs and outputs, and these mathematical operations are called properads. At the end of the training, I put a slide for further reading.

# Linux API and Languages

# Linux APl and Languages

2023 Software Diagnostics Services

Now we look at how Linux API is used by various languages other than C and $\mathsf { C } { + } { + }$ . The choice of languages was dictated by my own interests. Fortunately, all of them are mainstream. I’ve chosen the simple example of calling the execve syscall wrapper function.

# APl and C#

Installation   
Platform Invoke

2023Software Diagnostics Services

Our first language is C#. The way to call Linux API is to use the so-called P/Invoke mechanism. Since .NET and C# are not the main Linux languages, I created an exercise with the appropriate installation steps.

Installation

https://learn.microsoft.com/en-us/dotnet/core/install/linux

Platform Invoke

https://learn.microsoft.com/en-us/dotnet/standard/native-interop/pinvoke

# Exercise L8

Goal: Install .NET environment and write a simple program that uses Linux APl   
\LAPI-Dumps\Exercise-L8.pdf

2023 Software Diagnostics Services

Goal: Install .NET environment and write a simple program that uses Linux API.

1. Install .NET SDK on your distribution (https://learn.microsoft.com/en-us/dotnet/core/install/linux). We use Debian 10 on WSL for demonstration (https://learn.microsoft.com/en-us/dotnet/core/install/linux-debian).

```txt
\~/LAPI\$ wget https://packages.microsoft.com/config/debian/10/packages-microsoft-prod deb -O packages-microsoft-prod.deb 
```

$\sim / \mathrm{LAPI}$ sudo dpkg -i packages-microsoft-prod.def

$\sim / \mathrm{LAPI}$ rm packages-microsoft-prod.deb

```txt
~/LAPI\$ sudo apt-get update 
```

$\sim / \mathrm{LAPI} \$ sudo apt-get install -y dotnet-sdk-7.0$

2. Verify that .NET SDK and runtime are installed.

```shell
~/LAPI\$ dotnet --list-sdks 
```

```json
7.0.302 [/usr/share/dotnet/sdk] 
```

```latex
\(\sim / \text{LAPI} \\) \text{dotnet} --list - runtimes\) 
```

```txt
Microsoft. AspNetCore. App 7.0.5 [/usr/share/dotnet/shared/Microsoft. AspNetCore. App] 
```

```htaccess
Microsoft.NETCore.App 7.0.5 [/usr/share/dotnet/shared/Microsoft.NETCore.App] 
```

3. Create SBT native project:

$\sim / \mathrm{LAPI}$ mkdir csharp-lapi

$\sim / \mathrm{LAPI}$ cd csharp-lapi

```txt
~/LAPI/csharp-lapi\$ dotnet new console 
```

```txt
Welcome to .NET 7.0! 
```

```txt
SDK Version: 7.0.302 
```

```txt
Telemetry 
```

The .NET tools collect usage data in order to help us improve your experience. It is collected by Microsoft and shared with the community. You can opt-out of telemetry by setting the DOTNET_CLI_TELEMETRY_OPTOUT environment variable to '1' or 'true' using your favorite shell.

Read more about .NET CLI Tools telemetry: https://aka.ms/dotnet-cli-telemetry

Installed an ASP.NET Core HTTPS development certificate.

To trust the certificate run 'dotnet dev-certs https --trust' (Windows and macOS only).

Learn about HTTPS: https://aka.ms/dotnet-https

Write your first app: https://aka.ms/dotnet-hello-world

Find out what's new: https://aka.ms/dotnet-whats-new

Explore documentation: https://aka.ms/dotnet-docs

Report issues and find source on GitHub: https://github.com/dotnet/core

Use 'dotnet --help' to see available commands or visit: https://aka.ms/dotnet-cli

The template "Console App" was created successfully.

Processing post-creation actions...

Restoring /home/coredump/LAPI/csharp-lapi/csharp-lapi.csproj:

Determining projects to restore...

Restored /home/coredump/LAPI/csharp-lapi/csharp-lapi.csproj (in 142 ms).

Restore succeeded.

4. Open Program.cs and replace code with:

using System.Runtime.InteropServices;

```txt
class LapiTest   
{ [DllImport("libc")] public static extern int execve(string _path, string[] __argv, string[] __envp); public static void Main() { execve("/bin/ps", new string[0], new string[0]); }   
} 
```

5. Run the project:

~/LAPI/csharp-lapi/lapi$ dotnet run

PID TTY TIME CMD

37187 pts/2 00:00:00 bash

44981 pts/2 00:00:06 dotnet

45267 pts/2 00:00:02 dotnet

45300 pts/2 00:00:00 ps

# APl and Scala Native

Documentation   
Scala Native   
Native code interoperability

2023Software Diagnostics Services

My second language choice was Scala, the language I started learning more than 3 years ago. It was originally created for JVM, but there’s also a native implementation using LLVM. Since the choice of language is rather unusual, I created a special exercise for it that also details the required Scala Native installation steps. Over the last 3 years, Scala Native has really improved.

# Documentation

https://buildmedia.readthedocs.org/media/pdf/scala-native/latest/scala-native.pdf

# Scala Native

https://scala-native.org/en/stable/index.html

# Native code interoperability

https://scala-native.org/en/stable/user/interop.html

# Exercise L9

Goal: Install Scala Native environment and write a simple program that uses Linux APl   
\LAPI-Dumps\Exercise-L9.pdf

2023Software Diagnostics Services

Goal: Install Scala Native environment and write a simple program that uses Linux API.

1. Install JDK.

```txt
~/LAPI\$ sudo apt update 
```

$\sim / \mathrm{LAPI}$ sudo apt install openjdk-11-jdk

```powershell
~/LAPI$ java --version
openjdk 11.0.18 2023-01-17
OpenJDK Runtime Environment (build 11.0.18+10-post-Debian-1deb10u1)
OpenJDK 64-Bit Server VM (build 11.0.18+10-post-Debian-1deb10u1, mixed mode, sharing) 
```

2. Install Scala with cs setup as recommended at https://www.scala-lang.org/download/.

~/LAPI$ curl -fL https://github.com/coursier/coursier/releases/latest/download/cs-x86_64-pclinux.gz | gzip -d > cs && chmod $\mathbf { + x }$ cs && ./cs setup   
Checking if a JVM is installed Found a JVM installed under /usr/lib/jvm/java-11-openjdk-amd64.   
```matlab
% Total % Received % Xferd Average Speed Time Spent Time Left Current Dload Upload Speed Speed 0 0 0 0 0 0 ------ -- ---- 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 19.7M 100 19.7M 0 0 10.6M 0 0 0:00:01 0:00:01 ---- ---- -27.6M 
```

```txt
Checking if ~/.local/share/coursier/bin is in PATH
Should we add ~/.local/share/coursier/bin to your PATH via ~/.profile? [Y/n] 
```

Checking if the standard Scala applications are installed   
```txt
Installed ammonite  
Installed cs  
Installed coursier  
InstalledScala  
Installed scalac  
Installedscala-cli  
Installed sbt  
Installed sbtn  
Installed scalafm 
```

$\sim / \mathsf { L A P I } \$ \Phi$ scala -version   
```txt
Scala code runner version 3.3.0 -- Copyright 2002-2023, LAMP/EPFL 
```

Note: You may need to log out and log in to make installation changes into effect.

3. Install clang:

```txt
~/LAPI$ clang --version  
clang version 7.0.1-8+deb10u2 (tags/RELEASE_701/final)  
Target: x86_64-pc-linux-gnu  
Thread model: posix  
InstalledDir: /usr/bin 
```

4. Create SBT native project:

```txt
\~/LAPI$ mkdir scalpalapi   
\~/LAPI$ cd scalpalapi   
\~/LAPI/scala-lapi$ sbt new scalp-native/scala-native.g8   
A minimal project that uses Scala Native.   
name [Scala Native Seed Project]: lapi   
Template applied in /home/coredump/LAPI/scala-lapi/../lapi   
\~/LAPI/scala-lapi$ cd lapi 
```

5. Open \src\main\scala\Main.scala and replace code with:

import scal.scalanativeunsafe._   
@external   
object libc{ def execve(_path:CString，_argv:Ptr[CString]，_envp:Ptr[CString]):Int $=$ extern   
}   
object Main{ def main(args:Array<String>):Unit $\equiv$ libc.execve(c"/bin/ps"，0.toPtr，0.toPtr)   
}

6. Run the project from sbt:

```txt
~/LAPI/scala-lapi/lapi\$ sbt run   
[info] welcome to sbt 1.8.3 (Debian Java 11.0.18)   
[info] loading settings for project lapi-build from plugins.sbt ...   
[info] loading project definition from /home/coredump/LAPI/scala-lapi/project   
[info] loading settings for project lapi from build.sbt ...   
[info] set current project to lapi (in build file:/home/coredump/LAPI/scala-lapi/)   
[info] compiling 1 Scala source to /home/coredump/LAPI/scala-lapi/target/scala-   
3.2.2/classes ...   
[info] Linking (1586 ms)   
[info] Discovered 671 classes and 3742 methods   
[info] Optimizing (debug mode) (3037 ms)   
[info] Generating intermediate code (1352 ms)   
[info] Produced 8 files   
[info] Compiling to native code (3526 ms)   
[info] Total (9745 ms)   
PID TTY TIME CMD   
37187 pts/2 00:00:00 bash   
41051 pts/2 00:01:26 java   
41916 pts/2 00:00:00 ps   
[success] Total time: 16 s, completed May 31, 2023, 10:39:08 PM 
```

7. Run the native executable:

```batch
\~/LAPI/scala-lapi/lapi\$ ./target/scala-3.2.2/lapi-out PID TTY TIME CMD 37187 pts/2 00:00:00 bash 43076 pts/2 00:00:00 ps 
```

# APl and Golang

unix   
Example:

```go
package main   
import ( "golang.org/x/sys UNIX"   
）   
func main() { uint.Exec("/bin/ps",nil,nil)   
}
```

2023Software Diagnostics Services

Another popular language choice is Golang, which I recently used in conjunction with $\mathsf { C } { + } { + }$ for some projects. There’s a unix package, and its simple usage is illustrated on this slide. I haven’t created an explicit exercise for this mainstream Linux language for cloud environments.

# APl and Rust

Unsafe: libc   
Safe: nix, rustix   
Example:

```rust
use libc::{c_char, execve};   
fn main() { unsafe { execve("/bin/ps".as_ptr() as *const c_char, std::ptr::null(), std::ptr::null()); }   
} 
```

2023Software Diagnostics Services

Another of my favorite languages is Rust. The example here uses the unsafe crate libc. Other crates are safe wrappers. I haven’t created an explicit exercise for this language, so please follow the official language installation documentation.

libc

https://lib.rs/crates/libc

nix

https://lib.rs/crates/nix

rustix

https://lib.rs/crates/rustix

# APl and Python

ctypes   
Example:

import ctypes

ctypes.CDLL("libc.so.6").execve("/bin/ps".encode("utf-8")，0，0)

2023Software Diagnostics Services

And finally, Python, where the ctypes library can be used if the higher level os package is not sufficient. The code is the simplest among all examples. The provided library link documentation has additional examples. I haven’t created an explicit exercise for this mainstream Linux language because, usually, Python interpreter is already included in many Linux distributions.

os library

https://docs.python.org/3/library/os.html

ctypes library

https://docs.python.org/3/library/ctypes.html

# Linux API Classes

# Linux API Classes

2023 Software Diagnostics Services

The final section of this training is a tour through selected Linux API classes. I don’t use the term category to avoid confusion with category theory. I chose API classes based on my experience with monitoring and also reading various Linux API books. Most of the classes have two sections: Library and Syscalls. Library API may use the corresponding syscalls in their implementation. Most likely, the second edition of this course will have more classes added. The goal of this section is to give a bird’s eye overview of API possibilities to stimulate further study and perhaps fill some gaps if you have.

# Tracing

strace (syscalls, signals)   
ltrace (libraries, syscalls, signals)   
Examples

strace bash   
ltrace bash   
ltrace -S bash

2023Software Diagnostics Services

There are two tools that help in seeing the dynamic usage of Linux API in processes, both library and system calls. The ltrace tool can log both types.

strace

https://man7.org/linux/man-pages/man1/strace.1.html

ltrace

https://man7.org/linux/man-pages/man1/ltrace.1.html

# Classification

Classification and Grouping of Linux System Calls   
A study of modern Linux APl usage and compatibility (slides)   
A study of modern Linux APl usage and compatibility (paper)

2023Software Diagnostics Services

There were a few attempts to classify Linux API, primarily syscalls.

Classification and Grouping of Linux System Calls

http://seclab.cs.sunysb.edu/sekar/papers/syscallclassif.htm

A study of modern Linux API usage and compatibility (slides)

https://oscarlab.github.io/api-compat-study/files/syspop-eurosys16-slides.pdf

A study of modern Linux API usage and compatibility (paper)

https://www.chiachetsai.com/files/eurosys16.pdf

# System Configuration APl

Library

. Invariant and increasable sysconf   
? Pathnames (f)pathconf

Syscalls

Identification uname

2023Software Diagnostics Services

System configuration API allows querying various properties and information about the kernel, such as version.

# File I/O API

File I/O Essentials   
Layers

Stream-based (open|seek|read write|close) FILE*   
Syscall-based creat|open|lseek|read|write|ioctl|close fd

Buffering

Block-based f(read|write)   
Line-based (f)puts,(f)printf   
Unbuffered (p)read(v),(p)write(v)

Multiplexing (p)select，poll,epoll_(create|ctl|wait)

2023Software Diagnostics Services

Since everything (or almost everything) is a file in Linux, we start with File I/O. I provided a link to the excellent free chapter on such API with many hands-on examples. Besides the usual split into library and syscall layers, we can architecturally view them as stream and file-descriptor based. Another API classification is possible when we take into account buffering. For line-based buffering examples, I provided only two examples, and there are many more functions. It is also possible to do multiplexing, waiting for data from many file descriptors at once.

# File Control API

# Library

Mixing FILE* and fd fileno, fdopen   
? Buffering set(v)buf, fflush   
Temp mkstemp, tmpfile

# Syscalls

Buffering (data)sync   
Access posix_fadvise   
Control fcntl   
Locking flock   
Truncation (f)truncate

2023Software Diagnostics Services

File control API allows changing the behavior of file I/O, such as set buffering parameters, and also provides conversion between file streaming structure and file descriptor. I also included functions for making temporary file names and files.

# Filesystem API

# Library

Directories (n)ftw, (fd)(open|read)dir, remove,get(c)wd   
? Paths realpath, (dirl base )name

# Syscalls

Mounting (u)mount   
Metadata (f|1)stat(at|vfs)   
Attributes (list|get|set)(f|1)xattr   
Permissions (1|f)ch(own|mod), umask   
Time utime   
Links (un)link, rename   
Symlinks (sym|read)link   
Directories (mk|rm|ch)dir, chroot   
Monitoring inotify_(init|(add|rm)_watch)

2023Software Diagnostics Services

Files are organized into filesystem hierarchies. There are several library functions and syscalls to mount filesystems, modify metadata, extended attributes, file and directory permissions, get file access and modification times, create hard and soft links, and do file activity monitoring.

# Dynamic Memory APl

# Library

Control mall(opt|info), memalign   
Allocation (m|c|re)alloc   
Deallocation free   
Debugging m(un)trace, m(check|probe)   
Stack alloca

# Syscalls

Adjustment (s )brk

2023Software Diagnostics Services

In this dynamic memory class, I included both heap and stack. Heap API is the most famous API from a software diagnostics perspective. The syscall only manages the heap region as a whole. The actual heap dynamic memory implementation is entirely in the library. Please note various useful debugging library functions. For small dynamic arrays and structures, it is sometimes faster to use local stack memory.

# Virtual Memory API

Library

Usage posix_madvise

Syscalls

Mapping m((un|re)map|sync)， remap) file_pages   
Protection mprotect   
Locking m(un)lock(all)   
. Residence mincore   
Usage madvise

2023Software Diagnostics Services

Virtual memory API allows mapping files to memory for faster access and allocation of memory regions with different protections.

# Shared Libraries API

# Library

Loading dlopen   
Unloading dlclose   
Errors dlerror   
Symbols dl(addr|sym)

2023Software Diagnostics Services

Shared library API uses virtual memory API to load .so files. It also includes functions to query addresses of symbols, for example, exported functions.

# Process API

# Library

Execution exec(v(p)|l(e|p)), fexecve,system   
Termination exit   
Exit (atlon_)exit   
Capabilities cap. ((get (set_)proc|free)

# Syscalls

Creation fork, clone   
Execution execve   
Termination _exit   
Waiting wait(pid|id|3|4)   
Resources acct, getrusage, (get|set)rlimit   
Priority(get|set)priority,sched_get_priority_(min|max)   
Scheduling sched_((set|get)(scheduler|param)lyield|rr_get_interval)   
Affinity sched_(set|get)affinity   
Capabilities prctl

2023Software Diagnostics Services

Process API includes functions to create, execute, and terminate processes, as well as to wait for them to finish their execution. The clone syscall allows for fine-grain control over process creation than just fork. Additional functions exist to set termination handlers, get resource usage and set their resource limits, priority, scheduling parameters, CPU affinity, and capabilities.

# IPC API

# Library

Pipes p(open close) FILE*   
FIFO mkfifo   
Keys ftok   
POSIX (mql sem shm) (open|unlink *

# Syscalls

Pipes pipe   
Handles dup(2)， close  
Message queues msg(get |snd|rcv|ctl)   
Semaphores sem(get|ctl|op)   
Shared memory shm(get|at|ct1|dt)

2023Software Diagnostics Services

Inter-process communication API includes functions for pipes, queues, shared memory, and semaphores for synchronization.

# Job API

Library

Terminal ctermid, tc(get|set)pgrp

Syscalls

Groups (get|set)pgid   
Sessions (get| set)sid

2023Software Diagnostics Services

Processes may be organized into groups and groups into sessions. Some processes may have controlling terminals for console I/O.

# Signals API

# Library

Sets sig(((empty|fill|add|del|and|or|isempty)set)|ismember)   
Sending raise,killpg,abort   
Description strsignal, psignal   
Waiting pause   
BSD sig(vec|block|(get|set)mask|pause)

# Syscalls

Disposition signal, sigaction   
Mask sig(procmask|pending)   
Sending kill,sigqueue   
Waiting sig(suspend|waitinfo), signalfd   
Stack sigaltstack

2023Software Diagnostics Services

Signals are also a way for interprocess communication as well as notification when something illegal happens, like invalid memory access. They are conceptually similar to software interrupts and exceptions with the possibility to set a separate stack for processing.

# Thread API

# Library

. One-time initialization pthread_once   
Creation pthread_(create|atfork)   
Termination pthread_exit   
Identification pthread_(self|equal   
Wait pthread_(join|detach)   
C Cancellation pthread_(set|test)cancel(state|type)   
Cleanup pthread_cleanup_(push|pop)   
Signals pthread_(sigmask|kill|sigqueue)， sigwait  
Attributes pthread_attr_(init|set*|destroy)   
Synchonization pthread_mutex(_(init|(un|try|timed)lock|destroy) attr (init|set*|destroy)) pthread _cond (init|signal broadcast (timed)wait |destroy)   
Data pthread_(key_createl(set|get)specific)

2023Software Diagnostics Services

Threads (or lightweight processes) have their own library API. Internally, it uses process API syscalls. This library also includes mutexes and conditional variables.

# Networking API

# Library

Addresses (get|free)addrinfo,gai_strerror   
Names getnameinfo

# Syscalls

Sockets socket, bind, listen,accept,connect,close, shutdown   
. Streaming write, send, read, recv   
Datagrams recvfrom, sendto   
Messages (send| recv)msg   
. Files sendfile   
. Domain socketpair   
Addresses get(sock|peer)name   
Options (get|set)sockopt

2023Software Diagnostics Services

Networking API includes UNIX domain and TCP/IP sockets and functions to get address information. When preparing this slide, I myself found a few functions I didn’t know about.

# Time API

# Library

. Conversion (c|asc|strf|strp|get|local|mk)time   
. Zones tzset   
. Locales setlocale   
Correction adjtime   
C Process clock

# Syscalls

. Calendar (get|set)timeofday,(s)time   
. Process times

2023Software Diagnostics Services

Time API includes various conversion functions and also syscalls to get calendar system time and process times spent in user and kernel modes.

# Timers API

# Library

Low-res sleep   
Clock(clock|pthread)_getcpuclockid

# Syscalls

Intervals (get|set)itimer, timer_(create|settime|delete)   
Repeating timer_getoverrun   
Once alarm   
File timerfd_(createl(get|set)time)   
Clock clock_(get|set)(time|res)   
Hi-res(clock_)nanosleep

2023Software Diagnostics Services

Programs need some sleep and also alarm clocks.

# Tracing and Logging APl

# Library

Writing (open|(v)sys Iclose)log   
. Filtering setlogmask

# Syscalls

Process ptrace   
Performance and monitoring perf_event_open   
eBPF bpf

2023Software Diagnostics Services

There are several ways to trace syscalls and monitor performance. The oldest one is perf events API, and the more recent is eBPF API. System logging is implemented at the library level.

# Accounts API

# Library

Records getpw(nam|uid),getgr(nam|gid)   
Scanning getspnam, (set|get|end)spent   
Groups (init|get|set)groups   
Encryption crypt(_r)

# Syscalls

· ID(get|set)((r)e)(s)(ulg)id   
Filesystem setfs(ulg)id

2023Software Diagnostics Services

The accounts API class helps get user and group information, get and set their identity, as well as do password encryption.

# Terminal API

# Library

Identification isatty, ttyname   
C Attributes tc(get|set)attr   
Speed cf(get|set)(to|ti)speed   
Control tc(sendbreak|drain|flush|flow)   
. Windows ioctl   
Pseudo (posix_open|grant|unlock)pt, ptsname

2023Software Diagnostics Services

Terminal I/O is important if you want to control its aspects, for example, to provide input without an echo or control the size of the console window. Pseudo-terminals are important for various tools like ssh.

# References and Resources

# References and Resources

2023 Software Diagnostics Services

Now a few slides about references and resources for further reading.

# Resources (Construction)

Windows System Programming, Fourth Edition (Appendix B)   
Programming with POSIX Threads   
The Linux Programming Interface   
Hands-On System Programming with Linux   
Linux System Programming Techniques   
Advanced Programming in the UNIX Environment   
Effective TCP/IP Programming   
UNIX Systems Programming   
0 Low Level X Window Programming   
Wayland Architecture /Wayland Book

2023Software Diagnostics Services

I also compiled a list of books useful for studying various Linux API classes from a software construction perspective. Of all these books that are listed, most of them were at least partially read, and some were read from cover to cover. Windows system programming book is included because it provides a helpful appendix comparing Windows API with Linux API (recall natural transformation). One other book, The Linux Programming Interface, stands out and provides an initial reference for this training classification part. If you are interested in windowing and graphics, I put two such books, too, including the Wayland protocol and architecture, a replacement for the original X11 one.

Wayland Architecture

https://wayland.freedesktop.org/architecture.html

Wayland Book

https://wayland-book.com/

# Resources (Postconstruction)

WinDbg Help/WinDbq.orq (quick links)   
DumpAnalysis.org/SoftwareDiagnostics.Institute/PatternDiagnostics.com   
Debugqing.TV/YouTube.com/DebuggingTV/YouTube.com/PatternDiagnostics   
Foundations of Linux Debugging.Disassembling.and Reversing   
Foundations of ARM64 Linux Debugging.Disassembling.and Reversing   
Software Diagnostics Library   
Encyclopedia of Crash Dump Analysis Patterns,Third Edition   
Trace.Log. Text.Narrative,Data   
Memory Dump Analysis Anthology (Diagnomicon)

![](images/2abcb23f4b4ef67d159c9f4d1551217dd5bc6195e08d81ab75f66aeb0c57a4fc.jpg)

![](images/4718ce6c3b301c507cf5423372544f5a70947f4ac018c610aa996ff239b64be1.jpg)

2023Software Diagnostics Services

If you don’t have experience with assembly language, then the Foundations books teach you assembly language from scratch in the context of GDB. Since I no longer provide them for training attendees, this training includes an assembly language section for both CPU platforms.

WinDbg quick links

http://WinDbg.org

Software Diagnostics Institute

https://www.dumpanalysis.org

Software Diagnostics Services

https://www.patterndiagnostics.com

Software Diagnostics Library https://www.dumpanalysis.org/blog

Memory Dump Analysis Anthology (Diagnomicon) https://www.patterndiagnostics.com/mdaa-volumes

Debugging.TV http://debugging.tv/ https://www.youtube.com/DebuggingTV https://www.youtube.com/PatternDiagnostics

Foundations of Linux Debugging, Disassembling, and Reversing https://www.patterndiagnostics.com/practical-foundations-linux-debugging-disassembling-reversing

Foundations of ARM64 Linux Debugging, Disassembling, and Reversing https://www.patterndiagnostics.com/practical-foundations-arm64-linux-debugging-disassembling-reversing

Encyclopedia of Crash Dump Analysis Patterns, Third Edition https://www.patterndiagnostics.com/encyclopedia-crash-dump-analysis-patterns

Trace, Log, Text, Narrative, Data https://www.patterndiagnostics.com/trace-log-analysis-pattern-reference

# Resources (Training)

Accelerated Linux Core Dump Analysis. Third Edition   
Accelerated Linux Debugging4   
Accelerated Linux Disassembly.Reconstruction,and Reversing. Second Edition   
Accelerated Software Trace Analysis,Revised Edition, Part 1: Fundamentals and Basic Patterns

2023Software Diagnostics Services

This slide shows various Linux courses I developed over time. We mentioned some ADDR patterns from the Accelerated Linux Disassembly, Reconstruction, and Reversing course.

Accelerated Linux Core Dump Analysis, Third Edition https://www.patterndiagnostics.com/accelerated-linux-core-dump-analysis-book

Accelerated Linux Debugging4 https://www.patterndiagnostics.com/accelerated-linux-debugging-4d

Accelerated Linux Disassembly, Reconstruction, and Reversing, Second Edition https://www.patterndiagnostics.com/accelerated-linux-disassembly-reconstruction-reversing-book

Accelerated Software Trace Analysis, Revised Edition, Part 1: Fundamentals and Basic Patterns https://www.patterndiagnostics.com/accelerated-software-trace-analysis-part1

# Resources (Category Theory)

Applied category theory books that have chapters explaining category theory:

Conceptual Mathematics: A First Introduction to Categories   
The Joy of Abstraction: An Exploration of Math, Category Theory, and Life   
Category Theory for Programmers   
Categories for Software Engineering   
An Invitation to Applied Category Theory: Seven Sketches in Compositionality   
Life Itself: A Comprehensive Inquiry Into the Nature, Origin, and Fabrication of Life   
Category Theory for the Sciences   
Conceptual Mathematics and Literature: Toward a Deep Reading of Texts and Minds   
Diagrammatic Immanence: Category Theory and Philosophy   
Mathematical Mechanics: From Particle To Muscle   
Memory Evolutive Systems; Hierarchy, Emergence, Cognition   
Mathematical Structures of Natural Intelligence   
0 Sheaf Theory Through Examples   
Visual Category Theory

2023Software Diagnostics Services

As I promised when introducing category theory, this slide contains references to applied category theory books. I also highlighted three books that had some influence on me. For example, they provided a deeper understanding of applied aspects that resulted in a better understanding of mathematical aspects.

Category Theory for Programmers

https://github.com/hmemcpy/milewski-ctfp-pdf

Visual Category Theory

https://dumpanalysis.org/visual-category-theory