## Page 1

O'REILLY

Modern
Concurrency
in Java
VirtualThreads,Structured
Concurrency,and Beyond
A N M Bazlur Rahman

<!-- 图源: ./_Modern_Concurrency_in_Java_-_A_N_M_Bazlur_Rahman_assets/images/p001_scan.jpg -->

---

## Page 2

<span style="font-family:'LiberationSans-Bold';font-size:30.00pt;font-weight:bold;color:#000000">Modern Concurrency in Java</span>
<span style="font-family:'LiberationSerif';font-size:21.25pt;color:#555555">Virtual Threads, Structured Concurrency, and</span>

<span style="font-family:'LiberationSerif';font-size:21.25pt;color:#555555">Beyond</span>

<span style="font-family:'LiberationSerif-Bold';font-size:21.25pt;font-weight:bold;color:#000000">A N M Bazlur Rahman</span>

![](./_Modern_Concurrency_in_Java_-_A_N_M_Bazlur_Rahman_assets/images/p002_img01.png)

O'REILLY

---

## Page 3

<span style="font-family:'LiberationSans-Bold';font-size:15.00pt;font-weight:bold;color:#000000">Modern Concurrency in Java</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">by A N M Bazlur Rahman</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Copyright © 2025 A N M Bazlur Rahman. All rights reserved.</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Published by O’Reilly Media, Inc., 141 Stony Circle, Suite 195, Santa</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Rosa, CA 95401.</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">O’Reilly books may be purchased for educational, business, or sales</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">promotional use. Online editions are also available for most titles</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">(</span><span style="font-family:'LiberationSerif-Italic';font-size:15.00pt;font-style:italic;color:#8e0012">https://oreilly.com</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">). For more information, contact our</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">corporate/institutional sales department: 800-998-9938 or</span>
<span style="font-family:'LiberationSerif-Italic';font-size:15.00pt;font-style:italic;color:#000000">corporate@oreilly.com</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">.</span>

<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Acquisitions Editor: Louise Corrigan</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Development Editor: Angela Rufino</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Production Editor: Jonathon Owen</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Copyeditor: Audrey Doyle</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Proofreader: Miah Sandvik</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Indexer: nSight, Inc.</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Cover Designer: Karen Montgomery</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Cover Illustrator: José Marzan Jr.</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Interior Designer: David Futato</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Interior Illustrator: Kate Dullea</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">September 2025: First Edition</span>

<span style="font-family:'LiberationSans-Bold';font-size:15.00pt;font-weight:bold;color:#000000">Revision History for the First Edition</span>

---

## Page 4

<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">2025-09-16: First Release</span>

<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">See </span><span style="font-family:'LiberationSerif-Italic';font-size:15.00pt;font-style:italic;color:#8e0012">https://oreilly.com/catalog/errata.csp?isbn=9781098165413</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000"> for release</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">details.</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">The O’Reilly logo is a registered trademark of O’Reilly Media, Inc. </span><span style="font-family:'LiberationSerif-Italic';font-size:15.00pt;font-style:italic;color:#000000">Modern</span>
<span style="font-family:'LiberationSerif-Italic';font-size:15.00pt;font-style:italic;color:#000000">Concurrency in Java</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">, the cover image, and related trade dress are</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">trademarks of O’Reilly Media, Inc.</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">The views expressed in this work are those of the author and do not</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">represent the publisher’s views. While the publisher and the author have</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">used good faith efforts to ensure that the information and instructions</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">contained in this work are accurate, the publisher and the author disclaim all</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">responsibility for errors or omissions, including without limitation</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">responsibility for damages resulting from the use of or reliance on this</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">work. Use of the information and instructions contained in this work is at</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">your own risk. If any code samples or other technology this work contains</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">or describes is subject to open source licenses or the intellectual property</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">rights of others, it is your responsibility to ensure that your use thereof</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">complies with such licenses and/or rights.</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">978-1-098-16541-3</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">[LSI]</span>

---

## Page 5

<span style="font-family:'LiberationSans-Bold';font-size:15.00pt;font-weight:bold;color:#000000">Dedication</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">To my daughter, Rushda, whose love, growth, and boundless curiosity</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">inspired the pages of this book. The sound of your laughter and your playful</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">interruptions motivated every word I wrote.</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">To my beloved wife, Tabassum, for your unwavering support and for</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">creating the loving space that allowed me to complete this journey.</span>

---

## Page 6

<span style="font-family:'LiberationSans-Bold';font-size:30.00pt;font-weight:bold;color:#000000">Preface</span>

<span style="font-family:'LiberationSans-Bold';font-size:21.25pt;font-weight:bold;color:#8e0012">Why I Wrote This Book</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Concurrency has long been one of the most challenging aspects of Java</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">development. It has consistently evolved to meet the demands of modern</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">software development while maintaining a strong commitment to backward</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">compatibility. Among all the advancements Java has introduced over the</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">years, Project Loom’s introduction of virtual threads marks a fundamental</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">shift in the world of concurrency.</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Concurrency is inherently challenging, and this difficulty has only increased</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">with the rise in performance demands. Even for seasoned developers,</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">managing it effectively remains a complex task. Today, modern applications</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">are primarily I/O-driven, as they interact with numerous other systems,</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">especially within the microservices architecture that dominates recent</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">software development to meet the growing demands of scalability.</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">I/O operations often take a significant amount of time. When a thread</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">makes an I/O call, it typically has to wait for the operation to complete,</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">which has been the traditional approach. While modern operating systems</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">can manage millions of open sockets, the number of available threads</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">remains limited. As a result, meeting the growing demand for higher</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">throughput with traditional threads has become increasingly complex.</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">In response to this limitation, various techniques and alternative</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">concurrency models have been devised, but each comes with its own set of</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">trade-offs. Virtual threads represent a promising solution by leveraging</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">lightweight, user-mode threads that can be multiplexed onto a smaller</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">number of kernel threads. This approach enables more efficient use of</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">system resources. It enhances scalability, marking a notable improvement in</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">the world of concurrency.</span>

---

## Page 7

<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">When I first encountered virtual threads, I was immediately intrigued. They</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">felt like a long-awaited breakthrough that could fundamentally change how</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">we write concurrent programs on the JVM, offering a simple and elegant</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">solution where Java previously lacked one.</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">I began experimenting with virtual threads, documenting my findings on</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">my blog, and speaking at conferences. The enthusiastic response from the</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">developer community reaffirmed that virtual threads were not just another</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">enhancement but a fundamental shift in the approach to concurrency.</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">As I continued to explore, I discovered a wealth of resources emerging</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">around virtual threads—official documentation, insightful blog posts,</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">GitHub repositories with real-world examples, and excellent conference</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">sessions. While these resources were individually valuable, each focused on</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">different aspects of the topic. Some addressed technical implementations,</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">while others discussed migration strategies and specific use cases.</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Through continuous writing, speaking, and hands-on work, I gained a</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">clearer and more comprehensive understanding of the subject. This</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">exploration provided a comprehensive understanding of the motivations</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">behind project Loom, its integration with Java’s existing concurrency</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">model, and its implications for building scalable and maintainable systems.</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">My respect for Java’s ability to adapt to change while maintaining</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">backward compatibility has only intensified throughout this journey.</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Eventually, I realized there was an opportunity to consolidate all this</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">understanding into a single, practical resource. This book is not just a</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">theoretical exploration but a practical, hands-on guide that gathers all the</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">necessary concepts, examples, and best practices into a single,</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">comprehensive resource.</span>

---

## Page 8

<span style="font-family:'LiberationSans-Bold';font-size:21.25pt;font-weight:bold;color:#8e0012">Who This Book Is For</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">This book is designed for Java developers who already possess a</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">foundational understanding of concurrency and multithreading. It is not a</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">beginner’s guide to these topics. Instead, it targets those who have</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">experience writing concurrent programs using traditional tools, such as</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">Thread</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">, </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">ExecutorService</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">, synchronization, and collection utilities like</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">ReentrantLock</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000"> and </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">Semaphore</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">, and are looking to deepen their</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">understanding of the modern concurrency features introduced in recent Java</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">releases, particularly virtual threads, structured concurrency, and scoped</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">values. If you’re looking to learn the fundamentals of concurrency, </span><span style="font-family:'LiberationSerif-Italic';font-size:15.00pt;font-style:italic;color:#000000">Java</span>
<span style="font-family:'LiberationSerif-Italic';font-size:15.00pt;font-style:italic;color:#000000">Concurrency in Practice</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000"> by Brian Goetz (Addison-Wesley Professional) is</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">still the recommended book.</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">If you’ve ever encountered challenges with thread exhaustion, blocking I/O,</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">thread pool tuning, or managing complex lifecycle and cancellation logic,</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">this book will help you rethink these issues in light of Java’s evolving</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">concurrency model. It is particularly beneficial for:</span>

<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Mid- to senior-level developers aiming to modernize their</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">concurrent code</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Architects designing scalable systems</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Performance-oriented engineers interested in building robust</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">concurrent applications</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Team leads assessing new technologies</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Anyone curious about the future of Java concurrency</span>

<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Junior developers with a basic understanding of Java will also find this</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">book useful for an overview of modern concurrency. However, a prior or</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">concurrent study of foundational topics such as synchronization, race</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">conditions, and data publishing is highly recommended for a complete</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">understanding. These fundamentals are vital for effectively writing</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">concurrent code in your applications. While some modern frameworks hide</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">these details from everyday developers, things become critical when</span>

---

## Page 9

<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">encountering a serious bug. Fundamental knowledge is always essential for</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">understanding the subject, and concurrency is no exception.</span>

---

## Page 10

<span style="font-family:'LiberationSans-Bold';font-size:21.25pt;font-weight:bold;color:#8e0012">What This Book Offers</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">This book consolidates everything I’ve learned about Project Loom and</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">virtual threads into one comprehensive resource. Inside, you will find:</span>

<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">An exploration of Java’s concurrency evolution, from platform</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">threads and the Executor framework to CompletableFuture and</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">reactive programming</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">A deep dive into the mechanics of virtual threads, structured</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">concurrency, and scoped values</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Practical, real-world examples that demonstrate how to apply these</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">new features effectively</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Extensive coverage of not only virtual threads but also structured</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">concurrency and scoped values</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Guidance on how modern frameworks like Spring Boot, Quarkus,</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">and Jakarta EE are integrating virtual threads</span>

<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Whether you are new to virtual threads or have been following Project</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Loom from its inception, you will discover valuable insights and practical</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">knowledge.</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">The examples and concepts in this book require at least JDK 21, as that is</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">when virtual threads became officially available. However, some chapters</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">discuss features that are still in preview or have been recently finalized.</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Therefore, having access to a newer JDK version, such as 24 or even 25,</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">will enable you to make the most of all the examples and discussions.</span>

---

## Page 11

<span style="font-family:'LiberationSans-Bold';font-size:21.25pt;font-weight:bold;color:#8e0012">How This Book Is Organized</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">This book is structured to take you on a journey from understanding the</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">importance of virtual threads to mastering their use in production</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">applications. Here’s what you can expect in each chapter:</span>
<span style="font-family:'LiberationSerif-Italic';font-size:15.00pt;font-style:italic;color:#8e0012">Chapter 1, “Introduction”</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">We begin by exploring the evolution of concurrency in Java,</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">tracing its development from platform threads through the</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">Executor framework, Fork/Join, and CompletableFuture,</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">with a brief introduction to reactive programming. This</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">historical context helps you appreciate why virtual threads</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">are a game changer. You’ll discover the trade-offs we’ve</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">made over the years and understand why Project Loom</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">takes a fundamentally different approach.</span>
<span style="font-family:'LiberationSerif-Italic';font-size:15.00pt;font-style:italic;color:#8e0012">Chapter 2, “Understanding Virtual Threads”</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">In this chapter, we introduce virtual threads in a hands-on</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">manner. You’ll learn what they are, how they differ from</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">platform threads, and how to create them. We explore</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">throughput improvements, scalability benefits, and practical</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">examples. Key topics include rate limiting with Semaphores</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">and important limitations such as pinning and ThreadLocal</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">considerations.</span>
<span style="font-family:'LiberationSerif-Italic';font-size:15.00pt;font-style:italic;color:#8e0012">Chapter 3, “The Mechanics of Modern Concurrency in Java”</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">This chapter dives deep into the mechanics of concurrency.</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">We investigate thread pools, the Executor framework, and</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">the ForkJoinPool that powers virtual threads. A key highlight</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">is building our own virtual thread implementation from</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">scratch using continuations. By creating NanoThreads, you’ll</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">gain a profound understanding of how virtual threads</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">operate under the hood.</span>

---

## Page 12

<span style="font-family:'LiberationSerif-Italic';font-size:15.00pt;font-style:italic;color:#8e0012">Chapter 4, “Structured Concurrency”</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">Here, we discuss one of Project Loom’s most significant</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">innovations: StructuredTaskScope. You’ll learn how it</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">addresses the problems of unstructured concurrency, master</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">different joining policies, handle exceptions gracefully, and</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">build robust concurrent applications. The chapter covers</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">everything from basic usage to custom joiners and nested</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">scopes. Please note that at the time of writing, structured</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">concurrency is still in preview. I hope this preview version</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">remains largely unchanged, but if there are updates, I will</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">address them in subsequent editions.</span>
<span style="font-family:'LiberationSerif-Italic';font-size:15.00pt;font-style:italic;color:#8e0012">Chapter 5, “Scoped Values”</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">Context propagation in highly concurrent applications has</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">always posed challenges. This chapter demonstrates how</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">scoped values provide a superior alternative to ThreadLocal</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">for virtual threads. You’ll learn the API, see practical</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">examples in the context of structured concurrency, and</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">understand when and how to transition from ThreadLocal.</span>
<span style="font-family:'LiberationSerif-Italic';font-size:15.00pt;font-style:italic;color:#8e0012">Chapter 6, “The Relevance of Reactive Java in Light of Virtual Threads”</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">In this chapter, we aim to understand the differences in</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">approaching concurrency between virtual threads and</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">reactive programming. We examine the differences between</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">blocking and nonblocking I/O, event-driven architecture,</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">and the trade-offs associated with each approach. This</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">chapter will help you make informed choices about when to</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">utilize virtual threads versus reactive frameworks.</span>
<span style="font-family:'LiberationSerif-Italic';font-size:15.00pt;font-style:italic;color:#8e0012">Chapter 7, “Modern Frameworks Utilizing Virtual Threads”</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">Explore how major frameworks are embracing virtual</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">threads. We cover Spring Boot, Quarkus, and Jakarta EE,</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">providing practical configuration examples and best</span>

---

## Page 13

<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">practices. This chapter bridges the gap between learning</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">about virtual threads and applying them in production</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">environments.</span>
<span style="font-family:'LiberationSerif-Italic';font-size:15.00pt;font-style:italic;color:#8e0012">Chapter 8, “Conclusion and Takeaways”</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">We conclude with key insights and a look at the future of</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">concurrent programming in Java.</span>
<span style="font-family:'LiberationSans-Bold';font-size:21.25pt;font-weight:bold;color:#8e0012">How to Read This Book</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">This book is designed to be read sequentially, as each chapter builds on</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">concepts from the previous ones. However, if you are already familiar with</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">certain topics, feel free to jump ahead to the chapters that interest you the</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">most.</span>
<span style="font-family:'LiberationSerif-Italic';font-size:15.00pt;font-style:italic;color:#000000">For beginners to virtual threads</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">Start with </span><span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#8e0012">Chapter 1</span><span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000"> to understand the historical context,</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">then proceed through each chapter in order.</span>
<span style="font-family:'LiberationSerif-Italic';font-size:15.00pt;font-style:italic;color:#000000">For experienced developers</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">If you prefer, you may skim </span><span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#8e0012">Chapter 1</span><span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000"> and proceed directly</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">to </span><span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#8e0012">Chapter 2</span><span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000"> for hands-on experience with virtual threads.</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">Don’t skip </span><span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#8e0012">Chapter 3</span><span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">; building virtual threads from scratch</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">provides invaluable insights.</span>
<span style="font-family:'LiberationSerif-Italic';font-size:15.00pt;font-style:italic;color:#000000">For architects and team leads</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">Pay special attention to Chapters </span><span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#8e0012">4</span><span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">–</span><span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#8e0012">5</span><span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">, which cover</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">structured concurrency and scoped values, as well as</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#8e0012">Chapter 6</span><span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">, which compares these concepts with reactive</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">programming. This will aid in making informed</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">architectural decisions.</span>

---

## Page 14

<span style="font-family:'LiberationSans-Bold';font-size:21.25pt;font-weight:bold;color:#8e0012">A Hands-On Approach</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Each chapter includes plenty of code examples, and I expect that as you</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">read, you’ll have your IDE open to try out the code examples one by one.</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Don’t just read the code; run it, modify it, break it, and fix it. There’s no</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">better way to internalize these concepts than through experimentation.</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">For your convenience, all source code is available at</span>
<span style="font-family:'LiberationSerif-Italic';font-size:15.00pt;font-style:italic;color:#8e0012">https://github.com/Modern-Concurrency-in-Java</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">. Each chapter comes with</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">its own package containing fully runnable examples. I strongly encourage</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">you to write the code yourself while reading the book. However, if you</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">prefer to skim and experiment with the code, feel free to clone the</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">repository before you begin reading.</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">If you have a technical question or a problem using the code examples,</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">please send an email to </span><span style="font-family:'LiberationSerif-Italic';font-size:15.00pt;font-style:italic;color:#8e0012">support@oreilly.com</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">.</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">This book is here to help you get your job done. In general, if example code</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">is offered with this book, you may use it in your programs and</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">documentation. You do not need to contact us for permission unless you’re</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">reproducing a significant portion of the code. For example, writing a</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">program that uses several chunks of code from this book does not require</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">permission. Selling or distributing examples from O’Reilly books does</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">require permission. Answering a question by citing this book and quoting</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">example code does not require permission. Incorporating a significant</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">amount of example code from this book into your product’s documentation</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">does require permission.</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">We appreciate, but generally do not require, attribution. An attribution</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">usually includes the title, author, publisher, and ISBN. For example:</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">“</span><span style="font-family:'LiberationSerif-Italic';font-size:15.00pt;font-style:italic;color:#000000">Modern Concurrency in Java</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000"> by A N M Bazlur Rahman (O’Reilly).</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Copyright 2025 A N M Bazlur Rahman, 978-1-098-16541-3.”</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">If you feel your use of code examples falls outside fair use or the</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">permission given above, feel free to contact us at </span><span style="font-family:'LiberationSerif-Italic';font-size:15.00pt;font-style:italic;color:#8e0012">permissions@oreilly.com</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">.</span>

---

## Page 15

<span style="font-family:'LiberationSans-Bold';font-size:21.25pt;font-weight:bold;color:#8e0012">Conventions Used in This Book</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">The following typographical conventions are used in this book:</span>
<span style="font-family:'LiberationSerif-Italic';font-size:15.00pt;font-style:italic;color:#000000">Italic</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">Indicates new terms, URLs, email addresses, filenames, and</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">file extensions.</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">Constant</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">width</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">Used for program listings, as well as within paragraphs to</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">refer to program elements such as variable or function</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">names, databases, data types, environment variables,</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">statements, and keywords.</span>
<span style="font-family:'LiberationMono-Bold';font-size:15.00pt;font-weight:bold;color:#000000">Constant width bold</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">Shows commands or other text that should be typed literally</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">by the user.</span>
<span style="font-family:'LiberationMono-Italic';font-size:15.00pt;font-style:italic;color:#000000">Constant width italic</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">Shows text that should be replaced with user-supplied</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">values or by values determined by context.</span>

<span style="font-family:'LiberationSans-Bold';font-size:14.99pt;font-weight:bold;color:#737373">TIP</span>

<span style="font-family:'LiberationSerif';font-size:11.25pt;color:#000000">This element signifies a tip or suggestion.</span>

<span style="font-family:'LiberationSans-Bold';font-size:14.99pt;font-weight:bold;color:#737373">NOTE</span>

<span style="font-family:'LiberationSerif';font-size:11.25pt;color:#000000">This element signifies a general note.</span>

---

## Page 16

<span style="font-family:'LiberationSans-Bold';font-size:14.99pt;font-weight:bold;color:#c67171">WARNING</span>

<span style="font-family:'LiberationSerif';font-size:11.25pt;color:#000000">This element indicates a warning or caution.</span>

<span style="font-family:'LiberationSans-Bold';font-size:21.25pt;font-weight:bold;color:#8e0012">O’Reilly Online Learning</span>

<span style="font-family:'LiberationSans-Bold';font-size:14.99pt;font-weight:bold;color:#737373">NOTE</span>

<span style="font-family:'LiberationSerif';font-size:11.25pt;color:#000000">For more than 40 years, </span><span style="font-family:'LiberationSerif-Italic';font-size:11.25pt;font-style:italic;color:#8e0012">O’Reilly Media</span><span style="font-family:'LiberationSerif';font-size:11.25pt;color:#000000"> has provided technology and business training,</span>
<span style="font-family:'LiberationSerif';font-size:11.25pt;color:#000000">knowledge, and insight to help companies succeed.</span>

<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Our unique network of experts and innovators share their knowledge and</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">expertise through books, articles, and our online learning platform.</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">O’Reilly’s online learning platform gives you on-demand access to live</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">training courses, in-depth learning paths, interactive coding environments,</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">and a vast collection of text and video from O’Reilly and 200+ other</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">publishers. For more information, visit </span><span style="font-family:'LiberationSerif-Italic';font-size:15.00pt;font-style:italic;color:#8e0012">https://oreilly.com</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">.</span>
<span style="font-family:'LiberationSans-Bold';font-size:21.25pt;font-weight:bold;color:#8e0012">How to Contact Us</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Please address comments and questions concerning this book to the</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">publisher:</span>

<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">O’Reilly Media, Inc.</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">141 Stony Circle, Suite 195</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Santa Rosa, CA 95401</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">800-889-8969 (in the United States or Canada)</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">707-827-7019 (international or local)</span>

---

## Page 17

<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">707-829-0104 (fax)</span>
<span style="font-family:'LiberationSerif-Italic';font-size:15.00pt;font-style:italic;color:#8e0012">support@oreilly.com</span>
<span style="font-family:'LiberationSerif-Italic';font-size:15.00pt;font-style:italic;color:#8e0012">https://oreilly.com/about/contact.html</span>

<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">We have a web page for this book, where we list errata and any additional</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">information. You can access this page at </span><span style="font-family:'LiberationSerif-Italic';font-size:15.00pt;font-style:italic;color:#8e0012">https://oreil.ly/modern-</span>
<span style="font-family:'LiberationSerif-Italic';font-size:15.00pt;font-style:italic;color:#8e0012">concurrency-java</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">.</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">For news and information about our books and courses, visit</span>
<span style="font-family:'LiberationSerif-Italic';font-size:15.00pt;font-style:italic;color:#8e0012">https://oreilly.com</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">.</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Find us on LinkedIn: </span><span style="font-family:'LiberationSerif-Italic';font-size:15.00pt;font-style:italic;color:#8e0012">https://linkedin.com/company/oreilly-media</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Watch us on YouTube: </span><span style="font-family:'LiberationSerif-Italic';font-size:15.00pt;font-style:italic;color:#8e0012">https://youtube.com/oreillymedia</span>
<span style="font-family:'LiberationSans-Bold';font-size:21.25pt;font-weight:bold;color:#8e0012">Acknowledgments</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">First and foremost, I am deeply grateful to Allah (glory be to Him, the</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Exalted) for granting me the strength, patience, and opportunity to complete</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">this book. Without His help, this journey would not have been possible.</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">I extend my heartfelt thanks to my wife and daughter, who made the</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">greatest sacrifice by allowing me the time to write, time we could have</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">spent together. Their support, patience, and love carried me through the</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">many long nights and weekends dedicated to this project. This book is as</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">much theirs as it is mine, and I am deeply grateful for their understanding</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">and unwavering support.</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">I would also like to thank my parents, whose pride in my work continues to</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">inspire me. Witnessing their joy with each book I write is one of my</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">greatest rewards and motivates me to continue pushing the boundaries of</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">my writing. Additionally, I am grateful to my father-in-law and mother-in-</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">law for their enthusiasm and encouragement regarding my work. Their</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">warm support and genuine excitement are absolutely invaluable to me.</span>

---

## Page 18

<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">I would like to express my appreciation to the reviewers, editors, and the</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">entire production team at O’Reilly. Thank you for your trust and tireless</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">efforts in shaping this book. To the Java community, your ideas,</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">discussions, and contributions surrounding Project Loom and modern</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">concurrency have been invaluable. Your role in helping me write a book</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">that is both practical and forward-looking cannot be overstated.</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Finally, to you, the reader: thank you for taking the time to pick up this</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">book. I hope it empowers you to write better, safer, and more modern</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">concurrent Java code. I look forward to hearing about your experiences</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">with the book and its impact on your work.</span>

---

## Page 19

<span style="font-family:'LiberationSans-Bold';font-size:30.00pt;font-weight:bold;color:#000000">Chapter 1. Introduction</span>

<span style="font-family:'LiberationSerif-Italic';font-size:15.00pt;font-style:italic;color:#000000">Concurrency is about dealing with lots of things at once. Parallelism is</span>
<span style="font-family:'LiberationSerif-Italic';font-size:15.00pt;font-style:italic;color:#000000">about doing lots of things at once.</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">To truly appreciate something, it is crucial to know how it came to be,</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">especially if we can discern the steps taken and the challenges overcome</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">along the way. This understanding not only highlights ongoing progress but</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">also helps us understand its relevance. Similarly, Java concurrency has</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">come a long way since its inception. ​It took a long time to evolve to its</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">current state. But if we want to understand recent advancements, such as</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">virtual threads and structured concurrency in modern Java, we must first</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">delve into its evolution. In this chapter, we will give you an initial view of</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Java concurrency and then briefly discuss how it has developed over time.</span>
<span style="font-family:'LiberationSans-Bold';font-size:21.25pt;font-weight:bold;color:#8e0012">A Brief History of Threads in Java</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Java was designed with concurrency in mind; it was one of the first</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">languages to provide built-in support for multithreading. Over the years,</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Java’s concurrency capabilities have been improved and refined, leaving</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">behind some potholes and lessons along the way.</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Java concurrency began with basic synchronization and thread</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">management. Then came the introduction of the </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#8e0012">java.util.concurrent</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">package in Java 5, which brought important new capabilities such as the</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#8e0012">Executor framework</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">, locks, and concurrent collections. Next, with the</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">introduction of the Fork/Join framework in Java 7, Java engaged the</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">concurrency performance of multicore processors. And most recently, with</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#8e0012">Project Loom</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">, Java addressed the complexity and limitations of traditional</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">thread-based concurrency, with lightweight, user-mode threads and</span>

<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">—Rob Pike</span>

---

## Page 20

<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">structured concurrency, ultimately aiming to make concurrent development</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">simpler and more efficient.</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Why does this evolution matter so much? As we uncover how Java’s</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">concurrency story unfolds, we discover a relentless pursuit of greater</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">efficiency and simplified programming in the face of ever-growing</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">complexity. This narrative extends beyond just Java; it reflects the</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">trajectory of software development and Java’s continued aspirations.</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Let’s dive deeper into understanding this evolution and appreciate the</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">strides made in Java concurrency.</span>
<span style="font-family:'LiberationSans-Bold';font-size:16.88pt;font-weight:bold;color:#000000">Java Is Made of Threads</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Concurrency was an explicit design goal of Java, a language that was</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">released with built-in thread capability and a threading feature that was a</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">key differentiator of the language. It removed developers’ dependency on</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">operating-system-specific features to achieve concurrency.</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">In Java, a thread is the smallest unit of execution. It is an independent path</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">of execution running within a program. Threads share the same address</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">space, meaning they have access to the same variables and data structures</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">of the program. However, each thread maintains its own program counter,</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">stack, and local variables, enabling it to operate independently. This design</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">also facilitates interaction among threads when necessary.</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">However, Java’s threading model relies on the underlying operating system</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">to schedule and execute threads. The operating system allocates CPU time</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">to each thread, managing the transition between threads to ensure efficient</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">execution. By distributing threads across multiple CPUs, the system can</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">achieve true parallelism, enhancing the performance of concurrent</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">applications (</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#8e0012">Figure 1-1</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">).</span>

---

## Page 21

<span style="font-family:'LiberationSerif-Italic';font-size:11.25pt;font-style:italic;color:#000000">Figure 1-1. Execution of threads by different CPUs</span>

<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">The more threads we can have in a Java program, the more execution</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">environment we effectively create. This gives us the ability to execute</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">several operations simultaneously. This is particularly beneficial for</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">applications that require high levels of parallelism or concurrency, such as</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">web servers, data processing pipelines, and real-time systems. By</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">leveraging multiple threads, these applications can perform multiple tasks</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">simultaneously, thereby improving throughput and responsiveness.</span>

![](./_Modern_Concurrency_in_Java_-_A_N_M_Bazlur_Rahman_assets/images/p021_img01.png)

---

## Page 22

<span style="font-family:'LiberationSans-Bold';font-size:14.99pt;font-weight:bold;color:#737373">PARALLELISM VERSUS CONCURRENCY</span>
<span style="font-family:'LiberationSerif';font-size:11.25pt;color:#000000">The terms </span><span style="font-family:'LiberationSerif-Italic';font-size:11.25pt;font-style:italic;color:#000000">parallelism</span><span style="font-family:'LiberationSerif';font-size:11.25pt;color:#000000"> and </span><span style="font-family:'LiberationSerif-Italic';font-size:11.25pt;font-style:italic;color:#000000">concurrency</span><span style="font-family:'LiberationSerif';font-size:11.25pt;color:#000000"> are often used interchangeably. However, they</span>
<span style="font-family:'LiberationSerif';font-size:11.25pt;color:#000000">mean two different things. Parallelism entails doing more than one thing simultaneously,</span>
<span style="font-family:'LiberationSerif';font-size:11.25pt;color:#000000">so multiple processing units, such as two or more CPU cores, are needed. Concurrency</span>
<span style="font-family:'LiberationSerif';font-size:11.25pt;color:#000000">is about designing programs where portions of their operation can overlap—even if not</span>
<span style="font-family:'LiberationSerif';font-size:11.25pt;color:#000000">always simultaneously. Think of parallelism as multiple workers building a house side-</span>
<span style="font-family:'LiberationSerif';font-size:11.25pt;color:#000000">by-side, while concurrency is like a single chef juggling multiple dishes in the kitchen.</span>
<span style="font-family:'LiberationSerif';font-size:11.25pt;color:#000000">Now, if we add more chefs to the kitchen, the overall output improves, but it is not</span>
<span style="font-family:'LiberationSerif';font-size:11.25pt;color:#000000">necessarily parallel; one chef might be chopping onions while another is putting a dish</span>
<span style="font-family:'LiberationSerif';font-size:11.25pt;color:#000000">in the oven. The end goal remains preparing the meal—perhaps even multiple meals.</span>

<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">The notion of threads forms the very fabric of how the entire Java</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">ecosystem is implemented and how it functions. It’s the bedrock on which</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">lots of powerful features and tools are based, and functions that many</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">programmers take for granted simply wouldn’t be possible without it.</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Whether it’s the garbage collection system, which tackles the problem of</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">memory management in Java, or the process of executing the simple output</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">of a “Hello, World!” program in Java, threads are working away in the</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">background.</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Here’s an example. Anyone who has written a Java program, even for the</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">first time, will be familiar with the line most programming books start out</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">with. It may seem like a single-threaded operation; however, the Java</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Virtual Machine (JVM) actually executes this code in a thread, commonly</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">referred to as the </span><span style="font-family:'LiberationSerif-Italic';font-size:15.00pt;font-style:italic;color:#000000">main thread</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">:</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">public class HelloWorld {</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">public static void main(String[] args) {</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">System.out.println(&quot;Hello,</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#cc3300"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">World!&quot;);</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">//</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#35586c"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">Displaying</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#35586c"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">the</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#35586c"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">thread</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#35586c"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">that’s</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#35586c"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">executing</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#35586c"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">the</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#35586c"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">main</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#35586c"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">method</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">System.out.println(&quot;Executed</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#cc3300"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">by</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#cc3300"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">thread:</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#cc3300"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">&quot;</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">+ Thread.currentThread().getName());</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">}</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">}</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">When we run this program, the output would include the name of the thread</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">executing the main method, which is typically </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">main</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">:</span>

---

## Page 23

<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">Hello, World!</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">Executed by thread: main</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">This example shows that even the most basic Java programs are already</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">fundamentally threaded. The implication is profound: we, as Java</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">developers, are harnessing the power of threads, whether we realize it or</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">not, in virtually every part of our job, from running the most</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">straightforward programs to using even the most advanced techniques, such</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">as garbage collection.</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">And so, threads are a required element of Java—they’re part of what makes</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Java a language that can scale up to handle large databases and massive</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">distributed systems.</span>
<span style="font-family:'LiberationSans-Bold';font-size:16.88pt;font-weight:bold;color:#000000">Threads: The Backbone of the Java Platform</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Java threads are integral to all layers of the Java platform, playing a crucial</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">role in various aspects beyond just executing code. For instance, they’re the</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">basis of exception handling, debugging, and profiling Java applications.</span>
<span style="font-family:'LiberationSans-Bold';font-size:15.00pt;font-weight:bold;color:#555555">Exceptions and threads</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">In Java, every thread has its own separate call stack that records all method</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">call invocations done during the thread’s lifetime. When an exception is</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">raised, that thread’s call stack becomes a vital part of the diagnostic history;</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">it shows the sequence of method invocations that led to the exception,</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">helping developers trace the root cause of an issue. Here’s that same</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">example again:</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">import java.sql.SQLException;</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">public class CallStackDemo {</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">public static void main(String[] args) throws InterruptedException {</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">Thread thread = new Thread(CallStackDemo::processOrder);</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">thread.setName(&quot;mcj-thread&quot;);</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">thread.start();</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">thread.join();</span>

---

## Page 24

<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">}</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">static void processOrder() {</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">validateOrderDetails();</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">}</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">static void validateOrderDetails() {</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">checkInventory();</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">}</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">static void checkInventory() {</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">updateDatabase();</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">}</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">static void updateDatabase() {</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">try {</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">throw new SQLException(&quot;Database</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#cc3300"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">connection</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#cc3300"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">error&quot;);</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">} catch (SQLException e) {</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">throw new InventoryUpdateException(&quot;Database</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#cc3300"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">Error:</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#cc3300"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">&quot; +</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">&quot;Unable</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#cc3300"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">to</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#cc3300"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">update</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#cc3300"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">inventory&quot;, e);</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">}</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">}</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">}</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">class InventoryUpdateException extends RuntimeException {</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">public InventoryUpdateException(String message, Throwable cause) {</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">super(message, cause);</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">}</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">}</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">In this scenario, the database update operation fails, which propagates back</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">up the call stack. The main thread starts calling </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">processOrder</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">, which</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">calls </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">validateOrderDetails</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">, which calls </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">checkInventory</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">, which</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">calls </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">updateDatabase</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">, which then throws the exception. We now will get</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">the following output in the console:</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">Exception in thread &quot;mcj-thread&quot;</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">ca.bazlur.mcj.chap1.InventoryUpdateException:</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">Database Error: Unable to update inventory</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">at</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">ca.bazlur.mcj.chap1.CallStackDemo.updateDatabase(CallStackDemo.java:33</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">)</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">at</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">ca.bazlur.mcj.chap1.CallStackDemo.checkInventory(CallStackDemo.java:26</span>

---

## Page 25

<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">)</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">at ca.bazlur.mcj.chap1.CallStackDemo.</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">validateOrderDetails(CallStackDemo.java:22)</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">at</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">ca.bazlur.mcj.chap1.CallStackDemo.processOrder(CallStackDemo.java:18)</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">at java.base/java.lang.Thread.run(Thread.java:1583)</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">Caused by: java.sql.SQLException: Database connection error</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">at</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">ca.bazlur.mcj.chap1.CallStackDemo.updateDatabase(CallStackDemo.java:31</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">)</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">... 4 more</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">This trace allows developers to inspect the call stack within a specific</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">thread context (i.e., the context of the </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">mcj-thread</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000"> thread where the</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">exception occurred). This granularity is highly beneficial in the JVM,</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">especially for debugging multithreaded applications where several threads</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">may run concurrently on the same or different tasks. This detail helps us</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">pinpoint issues faster, making debugging more focused and efficient, and</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">leading to a quicker resolution.</span>
<span style="font-family:'LiberationSans-Bold';font-size:15.00pt;font-weight:bold;color:#555555">Debugger and threads</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">How does the Java debugger figure out where exactly to pause the</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">execution of a running application? The answer again is threading. By</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">attaching itself to the various threads in the application, the debugger can</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">select which of these individual threads to examine or even change their</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">state while debugging an active application. This is fundamental to your</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">ability to find and fix bugs in applications where several threads might be</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">simultaneously executing at different stages, and where different threads</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">might be interacting in ways that need to be understood.</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Each action can be “stepped into,” “stepped over,” or “stepped out of” with</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">respect to a thread. One or more independent call stacks for the active</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">threads may be inspected in a debugging session. The call stack tells you</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">what happened before, such as what caused a thread to be at a given point</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">in a program. Understanding how a thread got to a certain state is</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">invaluable when you’re trying to figure out the cause of an unexpected</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">exception or an erroneous data manipulation.</span>

---

## Page 26

<span style="font-family:'LiberationSans-Bold';font-size:15.00pt;font-weight:bold;color:#555555">Profiler and threads</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Threads are equally vital to understanding Java’s operational dynamics, and</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">they play a crucial role in profiling. Just as threading facilitates pinpoint</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">precision in debugging, its utility extends to offering a granular perspective</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">in profiling practices. In fact, threads are the backbone of performance</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">analysis in Java. Profiling tools rely on threads to give you a detailed look</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">at how your application is running. They use thread information to pinpoint</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">slowdowns, troubleshoot tricky timing issues in multithreaded code, and</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">help you find ways to make your application run faster. In short, threads are</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">the key to understanding and improving the performance of your</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">multithreaded Java applications.</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Java threads play a significant role in many operations, such as diagnosis,</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">debugging, and profiling, by providing detailed insight into the execution of</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">individual thread-level programs. Despite not being directly used by</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">developers, they keep operating underneath and augment the benefits of</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Java applications as well. Examples of this are garbage collection threads</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">used for the management of memory; compiler threads in the just-in-time</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">(JIT) system for performance enhancement; signal dispatcher, finalizer, and</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">reference handler threads for smooth execution of JVM; and virtual</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">machine (VM) and service threads for the core JVM tasks and diagnostics.</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Hence, it is easy to say that Java threads are one of the most essential layers</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">of the Java platform, and a Java developer needs to understand them.</span>
<span style="font-family:'LiberationSans-Bold';font-size:21.25pt;font-weight:bold;color:#8e0012">The Genesis of Java 1.0 Threads</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Java 1.0 was released in 1996 and came with built-in support for threads.</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">This defining feature set it apart from many languages at that time. In the</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">early days, you could create threads by either extending the </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">Thread</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000"> class</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">or implementing the </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">Runnable</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000"> interface. For example:</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">public class ThreadCreationDemo {</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">public static void main(String[] args) {</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">//</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#35586c"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">Extending</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#35586c"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">Thread</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#35586c"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">class</span>

---

## Page 27

<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">Thread t1 = new ThreadByExtension(&quot;Worker-1&quot;);</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">t1.start();</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">//</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#35586c"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">Implementing</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#35586c"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">Runnable</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#35586c"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">(classic)</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">Thread t2 = new Thread(</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">new RunnableImplementation(), &quot;Worker-2&quot;);</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">t2.start();</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">//</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#35586c"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">Anonymous</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#35586c"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">inner</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#35586c"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">class</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#35586c"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">(Java</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#35586c"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">1.1)</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">Thread t3 = new Thread(new Runnable() {</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">@Override</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">public void run() {</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">System.out.println(&quot;Anonymous:</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#cc3300"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">&quot; +</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">Thread.currentThread().getName());</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">}</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">}, &quot;Worker-3&quot;);</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">t3.start();</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">//</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#35586c"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">Lambda</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#35586c"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">expression</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#35586c"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">(Java</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#35586c"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">8)</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">Thread t4 = new Thread(() -&gt;</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">System.out.println(&quot;Lambda:</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#cc3300"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">&quot; +</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">Thread.currentThread().getName()),</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">&quot;Worker-4&quot;);</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">t4.start();</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">}</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">}</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">class ThreadByExtension extends Thread {</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">public ThreadByExtension(String name) {</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">super(name);</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">}</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">@Override</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">public void run() {</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">System.out.println(&quot;Extended</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#cc3300"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">Thread:</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#cc3300"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">&quot; + getName());</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">}</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">}</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">class RunnableImplementation implements Runnable {</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">@Override</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">public void run() {</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">System.out.println(&quot;Runnable:</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#cc3300"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">&quot; +</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">Thread.currentThread().getName());</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">}</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">}</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Key design considerations from the start:</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">Thread extension offers direct access to thread methods but</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">uses up your single inheritance slot.</span>

![](./_Modern_Concurrency_in_Java_-_A_N_M_Bazlur_Rahman_assets/images/p027_img01.png)



![](./_Modern_Concurrency_in_Java_-_A_N_M_Bazlur_Rahman_assets/images/p027_img02.png)



![](./_Modern_Concurrency_in_Java_-_A_N_M_Bazlur_Rahman_assets/images/p027_img03.png)



![](./_Modern_Concurrency_in_Java_-_A_N_M_Bazlur_Rahman_assets/images/p027_img04.png)



![](./_Modern_Concurrency_in_Java_-_A_N_M_Bazlur_Rahman_assets/images/p027_img05.png)

---

## Page 28

<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">The </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">Runnable</span><span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000"> implementation became the preferred</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">approach, separating task definition from thread</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">management and preserving inheritance flexibility.</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">Anonymous inner classes (Java 1.1) reduced boilerplate for</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">one-off tasks before lambdas existed.</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">Lambda expressions (Java 8) made the </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">Runnable</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">implementation even more concise, since it’s a functional</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">interface.</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">This foundational API has remained unchanged for nearly three decades,</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">demonstrating its robust design. While Java has added countless</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">concurrency enhancements, from </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">java.util.concurrent</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000"> to virtual</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">threads, these original building blocks remain the foundation of Java’s</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">threading model.</span>
<span style="font-family:'LiberationSans-Bold';font-size:16.88pt;font-weight:bold;color:#000000">Starting Threads</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Whichever method we choose for creating a thread begins by invoking the</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">start</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000"> method on the </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">Thread</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000"> object. This is an essential step because</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">calling </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">start</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000"> does more than just execute the </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">run</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000"> method; it also</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">performs setup tasks like allocating system resources. The </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">start</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000"> method</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">subsequently calls the </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">run</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000"> method in a new thread of execution.</span>

<span style="font-family:'LiberationSans-Bold';font-size:14.99pt;font-weight:bold;color:#737373">NOTE</span>

<span style="font-family:'LiberationSerif';font-size:11.25pt;color:#000000">It’s crucial not to call the </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">run</span><span style="font-family:'LiberationSerif';font-size:11.25pt;color:#000000"> method directly. Doing so will execute the </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">run</span><span style="font-family:'LiberationSerif';font-size:11.25pt;color:#000000"> method in</span>
<span style="font-family:'LiberationSerif';font-size:11.25pt;color:#000000">the calling thread, not in a new thread.</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">However, in modern Java applications, using the Executor framework is</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">more efficient than manually creating threads through constructors.</span>

![](./_Modern_Concurrency_in_Java_-_A_N_M_Bazlur_Rahman_assets/images/p028_img01.png)



![](./_Modern_Concurrency_in_Java_-_A_N_M_Bazlur_Rahman_assets/images/p028_img02.png)



![](./_Modern_Concurrency_in_Java_-_A_N_M_Bazlur_Rahman_assets/images/p028_img03.png)

---

## Page 29

<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Executors abstract the process of thread management by maintaining a</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">thread pool—a group of pre-created threads ready for executing tasks. This</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">method significantly reduces the overhead associated with creating new</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">threads.</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">When a task is submitted to an executor, it’s placed in a queue. A thread</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">from the pool then picks up the task from the queue and executes it. This</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">setup simplifies concurrent programming and optimizes resource utilization</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">by reusing threads.</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Here’s a simple example demonstrating how to use an executor to manage a</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">thread pool:</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">import java.util.concurrent.ExecutorService;</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">import java.util.concurrent.Executors;</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">public class ExecutorExample {</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">public static void main(String[] args) {</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">try (ExecutorService executor =</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">Executors.newFixedThreadPool(5)) {</span>

<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">for (int i = 0; i &lt; 10; i++) {</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">final int taskId = i;</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">executor.submit(() -&gt; {</span>

<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">+ &quot;</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#cc3300"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">in</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#cc3300"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">thread</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#cc3300"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">&quot; +</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">Thread.currentThread().getName());</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">});</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">}</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">}</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">}</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">}</span>
<span style="font-family:'LiberationSans-Bold';font-size:21.25pt;font-weight:bold;color:#8e0012">Understanding the Hidden Costs of Threads</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Many of today’s web applications run on a thread-per-request model, where</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">a thread is assigned to each request—the thread manages the whole</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">request/response lifecycle. For instance, let’s consider the lifecycle of a</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">request on a typical web application running under a servlet container</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">(Apache Tomcat, Jetty, or any Java EE web server). A</span><span style="font-family:'LiberationSerif-Italic';font-size:15.00pt;font-style:italic;color:#000000"> servlet container</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000"> is</span>

<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">System.out.println(&quot;Executing</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#cc3300"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">task</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#cc3300"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">&quot; + taskId</span>

---

## Page 30

<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">software responsible for processing requests coming to the web application.</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">When a request arrives at the servlet container, the container assigns the</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">request to one of the threads in its thread pool to process the request. That</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">thread fires up, in effect saying, “Hey, I got this request coming from the</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">user. It’s mine now; I’ll take care of it.” The thread essentially takes</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">responsibility for processing the whole request/response lifecycle (</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#8e0012">Figure 1-</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#8e0012">2</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">).</span>

<span style="font-family:'LiberationSerif-Italic';font-size:11.25pt;font-style:italic;color:#000000">Figure 1-2. Thread pool handling servlet requests</span>

<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">This is particularly helpful when we have large numbers of simultaneous</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">connections because increasing concurrency, in turn, increases the</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">throughput of the web application. In fact, this is a key feature of the</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">thread-per-request model, which allows modern web applications to scale in</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">order to serve a growing volume of requests efficiently.</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Amazingly, most modern operating systems can handle millions of</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">concurrent connections, so it would seem reasonable to create yet more</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">threads in order to improve throughput. More threads in the thread pool</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">should indeed equate to more throughput, according to Little’s Law.</span><span style="font-family:'LiberationSans';font-size:11.25pt;color:#8e0012">1</span>

![](./_Modern_Concurrency_in_Java_-_A_N_M_Bazlur_Rahman_assets/images/p030_img01.png)

---

## Page 31

<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">However, even though that assumption appeared to be accurate, it turns out</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">that this line of reasoning is a bit slippery—and will not always produce the</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">expected results.</span>

<span style="font-family:'LiberationSans-Bold';font-size:14.99pt;font-weight:bold;color:#737373">NOTE</span>

<span style="font-family:'LiberationSerif-Italic';font-size:11.25pt;font-style:italic;color:#000000">Throughput</span><span style="font-family:'LiberationSerif';font-size:11.25pt;color:#000000"> in web applications is the rate at which requests are processed and the</span>
<span style="font-family:'LiberationSerif';font-size:11.25pt;color:#000000">server delivers responses, typically measured in requests per second (RPS) or</span>
<span style="font-family:'LiberationSerif';font-size:11.25pt;color:#000000">transactions per second (TPS). It indicates the application’s capacity to handle load,</span>
<span style="font-family:'LiberationSerif';font-size:11.25pt;color:#000000">serving as a critical metric for performance, scalability, and resource utilization.</span>
<span style="font-family:'LiberationSerif';font-size:11.25pt;color:#000000">Throughput can be calculated as</span>
<span style="font-family:'Unnamed-T3';font-size:11.68pt;color:#000000">Throughput =</span><span style="font-family:'Unnamed-T3';font-size:8.25pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:8.25pt;color:#000000">Total Request Processed</span>

<span style="font-family:'Unnamed-T3';font-size:8.25pt;color:#000000">Total Time</span>

<span style="font-family:'LiberationSerif';font-size:11.25pt;color:#000000">This serves as a guide for benchmarking performance, scalability, and demand planning,</span>
<span style="font-family:'LiberationSerif';font-size:11.25pt;color:#000000">and it paves the way for the optimal use of resources that ensure a quality user</span>
<span style="font-family:'LiberationSerif';font-size:11.25pt;color:#000000">experience.</span>

<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">An important detail to bear in mind is the memory footprint of each thread:</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">about 2 MiB</span><span style="font-family:'LiberationSans';font-size:11.25pt;color:#8e0012">2</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000"> of memory (outside the heap per thread). This can quickly</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">become a lot, especially in large-scale applications running thousands of</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">concurrent connections—the aggregate memory footprint of the threads can</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">make a world of difference in the actual number of connections you can</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">run. Additionally, suppose your application uses all physical memory. In</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">that case, you’ll find yourself paging to disk, a significantly slower</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">operation than even accessing RAM: the time-to-read difference when</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">reading from disk versus RAM is a factor of 1,000. This alone can heavily</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">impact performance.</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Also, it is essential to understand that Java threads are actually a thin</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">wrapper around the native threads provided by the host operating system.</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">This means that the maximum number of threads you can create for any</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">application is effectively limited by the native thread creation limit provided</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">by the host operating system. Almost all operating systems have an upper</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">limit on the number of threads that can be spawned. So an application’s</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">scalability potential is limited by this bottleneck.</span>

---

## Page 32

<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Besides, there is the expense of context switching. Thread creation isn’t just</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">a memory overhead. It’s also a CPU overhead. When you switch between</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">threads, that’s a context switch. The context of the current thread needs to</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">be stored, and the context of the new thread needs to be brought back. All</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">that context switching costs CPU cycles, and, on systems under high load,</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">this overhead can make a huge difference in performance. This is another</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">underappreciated source of weight in your application.</span>
<span style="font-family:'LiberationSans-Bold';font-size:16.88pt;font-weight:bold;color:#000000">How Many Threads Can You Create?</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Now that we have discussed the costs associated with threads, let’s examine</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">how to measure the limitation of thread creation in your environment. By</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">running a simple test program, you can determine the maximum number of</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">threads your system can handle before experiencing issues. Here’s a</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">skeleton code snippet to start:</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">import java.util.concurrent.atomic.AtomicInteger;</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">import java.util.concurrent.locks.LockSupport;</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">public class ThreadLimitTest {</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">public static void main(String[] args) {</span>

<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">var threadCount = new AtomicInteger(0);</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">try {</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">while (true) {</span>

<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">var thread = new Thread(() -&gt; {</span>

<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">threadCount.incrementAndGet();</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">LockSupport.park();</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">});</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">thread.start();</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">}</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">} catch (OutOfMemoryError error) {</span>

<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">System.out.println(&quot;Reached</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#cc3300"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">thread</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#cc3300"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">limit:</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#cc3300"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">&quot; +</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">threadCount);</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">error.printStackTrace();</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">}</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">}</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">}</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Let’s execute this program to see how many threads can be created before</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">running out of memory or hitting other system limitations:</span>

---

## Page 33

<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">Reached thread limit: 16363</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">java.lang.OutOfMemoryError: unable to create native thread: possibly</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">out of</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">memory or process/resource limits reached</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">at java.base/java.lang.Thread.start0(Native Method)</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">at java.base/java.lang.Thread.start(Thread.java:1526)</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">at</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">ca.bazlur.chapter0.ThreadLimitTest.main(ThreadLimitTest.java:15)</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">On my machine, I encountered an </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">OutOfMemoryError</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000"> after successfully</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">creating 16,363 threads. This experiment highlights the point that there’s a</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">finite limit to the number of threads one can create, a constraint that is</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">largely dictated by the underlying operating system and the hardware.</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Hence, the limit can be different on your machine.</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">As you can see, while threads provide substantial benefits in web</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">application scalability, they do come with associated costs. These costs,</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">though sometimes less obvious, should be carefully considered to ensure</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">optimal performance.</span>
<span style="font-family:'LiberationSans-Bold';font-size:21.25pt;font-weight:bold;color:#8e0012">Resource Efficiency in High-Scale</span>
<span style="font-family:'LiberationSans-Bold';font-size:21.25pt;font-weight:bold;color:#8e0012">Applications</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Today’s software applications are often expected to process large amounts</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">of data and handle high volumes of incoming traffic simultaneously. These</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">dynamics create a significant challenge for staying fiscally sound,</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">especially when the cloud has become the default environment in which</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">many businesses operate. Even the slightest degree of resource inefficiency</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">can quickly turn into escalating costs. Since we have only a finite number</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">of threads, it’s essential to use them carefully. But threads get blocked in</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">practice, so they’re often not used effectively.</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Consider the following example, where a method calculates a person’s</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">credit score based on various factors:</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">public Credit calculateCredit(Long personId) {</span>

---

## Page 34

<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">var person = getPerson(personId);</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">//</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#35586c"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">Database</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#35586c"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">call</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#35586c"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">-</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">blocks</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#35586c"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">thread</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">var assets = getAssets(person);</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">//</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#35586c"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">API</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#35586c"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">call</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#35586c"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">-</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#35586c"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">blocks</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">thread</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#35586c">  </span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">var liabilities = getLiabilities(person);</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">//</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#35586c"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">Database</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#35586c"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">call</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#35586c"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">-</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">blocks</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#35586c"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">thread</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">importantWork();</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">//</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#35586c"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">CPU-intensive</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#35586c"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">work</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">return calculateCredits(assets, liabilities);</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">}</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">The preceding code snippet shows a sequence of five method invocations,</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">each happening one after the other. Suppose each would typically take 200</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">milliseconds to complete. Then the time it takes for the</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">calculateCredit()</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000"> method to execute would be about 1 second—</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">roughly five times as long (1,000 milliseconds = 200 milliseconds × 5).</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">The real inefficiency here isn’t that the thread is idle, but rather that it’s</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">blocked during input/output (I/O) operations. When the thread executes</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">getPerson(personId)</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">, it makes a database call and must wait for the</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">network response. During this waiting period, the thread cannot be</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">reassigned to handle other requests or perform other work. The same</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">blocking occurs during the </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">getAssets()</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000"> and </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">getLiabilities()</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000"> calls.</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">While the thread is technically “busy” executing these methods, it’s</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">spending most of its time blocked on I/O operations rather than doing</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">productive computational work.</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">This blocking behavior represents a significant inefficiency: expensive</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">thread resources are tied up waiting for external systems to respond, when</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">they could potentially be serving other requests. In high-throughput</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">applications, this can severely limit scalability since each blocked thread</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">represents one fewer thread available to handle incoming requests.</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Dealing with the key challenge of achieving high thread efficiency, in</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">particular, trying to minimize the time that threads spend blocked on I/O</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">operations, has driven many of Java’s evolutions over time. This includes</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">support for various paradigms such as asynchronous method invocations</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">(allowing methods to run independently while I/O operations complete),</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">thread pooling (reusing a fixed number of threads to perform tasks), and,</span>

---

## Page 35

<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">more recently, modern reactive programming models (emphasizing non-</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">blocking, event-driven interactions).</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Let’s start by introducing the classical methods of threading, such as</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">manually creating and managing individual threads, and gradually move</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">into what modern offerings provide.</span>
<span style="font-family:'LiberationSans-Bold';font-size:16.88pt;font-weight:bold;color:#000000">The Parallel Execution Strategy</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Looking at our preceding code snippet, we can see that there are some</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">method invocations, namely, </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">getAssets()</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">, </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">getLiabilities()</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">, and</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">importantWork()</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">, that do not have interdependencies, so these methods</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">can actually be dispatched to run in parallel. This approach to parallel</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">execution will reduce the duration for which our main thread is blocked,</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">even though we haven’t totally eliminated blocking.</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Our parallel execution strategy effectively allows the main thread to</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">diminish idle time. This essentially means that, since the main thread is less</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">idle, once this </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">calculate</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">​</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">Credit()</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000"> method is done executing, it can</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">quickly be free to do other work.</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Let’s start by creating the basic data structures we’ll need for our credit</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">calculation system:</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">//</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#35586c"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">Credit</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#35586c"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">calculation</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#35586c"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">models</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">record Credit(double score) {}</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">record Person(Long id, String name) {}</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">record Asset(String type, double value) {}</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">record Liability(String type, double amount) {}</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Now, let’s implement our parallel execution approach:</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">import java.util.List;</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">import java.util.concurrent.Callable;</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">import java.util.concurrent.atomic.AtomicReference;</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">Credit calculateCreditWithUnboundedThreads(Long personId)</span>

<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">throws InterruptedException {</span>

---

## Page 36

<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">var person = getPerson(personId);</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">var assetsRef = new AtomicReference&lt;List&lt;Asset&gt;&gt;();</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">var t1 = new Thread(() -&gt; {</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">var assets = getAssets(person);</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">assetsRef.set(assets);</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">});</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">var liabilitiesRef = new AtomicReference&lt;List&lt;Liability&gt;&gt;();</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">Thread t2 = new Thread(() -&gt; {</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">var liabilities = getLiabilities(person);</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">liabilitiesRef.set(liabilities);</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">});</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">var t3 = new Thread(() -&gt; importantWork());</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">t1.start();</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">t2.start();</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">t3.start();</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">t1.join();</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">t2.join();</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">var credit = calculateCredits(assetsRef.get(),</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">liabilitiesRef.get());</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">t3.join();</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">return credit;</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">}</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Let’s see how the preceding code works:</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">Uses </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">AtomicReference</span><span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000"> to safely share data between threads.</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">Thread-safe assignment of results that can be accessed from</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">the main thread.</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">Independent work that can run in parallel without affecting</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">the credit calculation.</span>

![](./_Modern_Concurrency_in_Java_-_A_N_M_Bazlur_Rahman_assets/images/p036_img01.png)



![](./_Modern_Concurrency_in_Java_-_A_N_M_Bazlur_Rahman_assets/images/p036_img02.png)



![](./_Modern_Concurrency_in_Java_-_A_N_M_Bazlur_Rahman_assets/images/p036_img03.png)



![](./_Modern_Concurrency_in_Java_-_A_N_M_Bazlur_Rahman_assets/images/p036_img04.png)



![](./_Modern_Concurrency_in_Java_-_A_N_M_Bazlur_Rahman_assets/images/p036_img05.png)



![](./_Modern_Concurrency_in_Java_-_A_N_M_Bazlur_Rahman_assets/images/p036_img06.png)



![](./_Modern_Concurrency_in_Java_-_A_N_M_Bazlur_Rahman_assets/images/p036_img07.png)



![](./_Modern_Concurrency_in_Java_-_A_N_M_Bazlur_Rahman_assets/images/p036_img08.png)



![](./_Modern_Concurrency_in_Java_-_A_N_M_Bazlur_Rahman_assets/images/p036_img09.png)



![](./_Modern_Concurrency_in_Java_-_A_N_M_Bazlur_Rahman_assets/images/p036_img10.png)



![](./_Modern_Concurrency_in_Java_-_A_N_M_Bazlur_Rahman_assets/images/p036_img11.png)

---

## Page 37

<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">All three threads start executing concurrently, reducing</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">overall execution time.</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">Waits for assets and liabilities threads to complete before</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">proceeding with credit calculation.</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">Calculates credit using the results from parallel operations.</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">Ensures the independent work completes before method</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">returns.</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">We’ve used </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">AtomicReference</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000"> from the standard Java concurrency</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">package—a thread-safe data structure that contains a reference to a single</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">object. Since we are dealing with multiple threads, it is essential that we use</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">it.</span>

<span style="font-family:'LiberationSans-Bold';font-size:14.99pt;font-weight:bold;color:#737373">TIP</span>

<span style="font-family:'LiberationSerif';font-size:11.25pt;color:#000000">Use </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">AtomicReference</span><span style="font-family:'LiberationSerif';font-size:11.25pt;color:#000000"> when you need to share object references safely between</span>
<span style="font-family:'LiberationSerif';font-size:11.25pt;color:#000000">threads. For primitive values, consider using </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">Atomic</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#8e0012">​</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">Integer</span><span style="font-family:'LiberationSerif';font-size:11.25pt;color:#000000">, </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">AtomicLong</span><span style="font-family:'LiberationSerif';font-size:11.25pt;color:#000000">, or other</span>
<span style="font-family:'LiberationSerif';font-size:11.25pt;color:#000000">atomic primitives.</span>

<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">To complete our credit calculation service, we need to implement the</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">supporting methods. Each method simulates real-world operations like</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">database queries and API calls:</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">//</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#35586c"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">Simulated</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#35586c"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">methods</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#35586c"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">with</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#35586c"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">200ms</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#35586c"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">delay</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#35586c"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">each</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">private Person getPerson(Long personId) {</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">simulateDelay(200);</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">return new Person(personId, &quot;John</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#cc3300"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">Doe&quot;);</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">}</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">private List&lt;Asset&gt; getAssets(Person person) {</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">simulateDelay(200);</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">return List.of(</span>

![](./_Modern_Concurrency_in_Java_-_A_N_M_Bazlur_Rahman_assets/images/p037_img01.png)



![](./_Modern_Concurrency_in_Java_-_A_N_M_Bazlur_Rahman_assets/images/p037_img02.png)



![](./_Modern_Concurrency_in_Java_-_A_N_M_Bazlur_Rahman_assets/images/p037_img03.png)

---

## Page 38

<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">new Asset(&quot;House&quot;, 300000),</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">new Asset(&quot;Car&quot;, 25000)</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">);</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">}</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">private List&lt;Liability&gt; getLiabilities(Person person) {</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">simulateDelay(200);</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">return List.of(</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">new Liability(&quot;Mortgage&quot;, 200000),</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">new Liability(&quot;Credit</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#cc3300"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">Card&quot;, 5000)</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">);</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">}</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">private void importantWork() {</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">simulateDelay(200);</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">System.out.println(&quot;Important</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#cc3300"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">work</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#cc3300"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">completed&quot;);</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">}</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">private Credit calculateCredits(List&lt;Asset&gt; assets,</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">List&lt;Liability&gt; liabilities) {</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">simulateDelay(200);</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">double totalAssets =</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">assets.stream().mapToDouble(Asset::value).sum();</span>

<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">double totalLiabilities = liabilities.stream()</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">.mapToDouble(Liability::amount)</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">.sum();</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">double creditScore = (totalAssets - totalLiabilities) / 1000;</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">return new Credit(creditScore);</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">}</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">private void simulateDelay(int milliseconds) {</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">try {</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">Thread.sleep(milliseconds);</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">} catch (InterruptedException e) {</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">Thread.currentThread().interrupt();</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">throw new RuntimeException(e);</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">}</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">}</span>

---

## Page 39

<span style="font-family:'LiberationSans-Bold';font-size:14.99pt;font-weight:bold;color:#737373">HANDLING INTERRUPTEDEXCEPTION</span>

<span style="font-family:'LiberationSans-Bold';font-size:14.99pt;font-weight:bold;color:#737373">CORRECTLY</span>

<span style="font-family:'LiberationSerif';font-size:11.25pt;color:#000000">When dealing with </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">InterruptedException</span><span style="font-family:'LiberationSerif';font-size:11.25pt;color:#000000"> in Java, it’s crucial to preserve the</span>
<span style="font-family:'LiberationSerif';font-size:11.25pt;color:#000000">thread’s interrupted status. The pattern shown demonstrates the proper way to handle</span>
<span style="font-family:'LiberationSerif';font-size:11.25pt;color:#000000">this exception:</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">catch (InterruptedException e) {</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">Thread.currentThread().interrupt();</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">throw new RuntimeException(e);</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">}</span>
<span style="font-family:'LiberationSerif';font-size:11.25pt;color:#000000">When a thread is interrupted via </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">Thread.interrupt()</span><span style="font-family:'LiberationSerif';font-size:11.25pt;color:#000000"> sets an internal flag, causing</span>
<span style="font-family:'LiberationSerif';font-size:11.25pt;color:#000000">blocking methods (such as </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">sleep()</span><span style="font-family:'LiberationSerif';font-size:11.25pt;color:#000000">, </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">wait()</span><span style="font-family:'LiberationSerif';font-size:11.25pt;color:#000000">, or blocking I/O operations) to throw</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">InterruptedException</span><span style="font-family:'LiberationSerif';font-size:11.25pt;color:#000000">. However, catching this exception clears the interrupted</span>
<span style="font-family:'LiberationSerif';font-size:11.25pt;color:#000000">flag, which can cause problems for code higher up the call stack that needs to know</span>
<span style="font-family:'LiberationSerif';font-size:11.25pt;color:#000000">about the interruption.</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">Thread.currentThread().interrupt()</span><span style="font-family:'LiberationSerif';font-size:11.25pt;color:#000000"> resets the interrupted flag on the current</span>
<span style="font-family:'LiberationSerif';font-size:11.25pt;color:#000000">thread. This ensures that any calling code checking </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">Thread.interrupted()</span><span style="font-family:'LiberationSerif';font-size:11.25pt;color:#000000"> or</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">Thread.currentThread().isInterrupted()</span><span style="font-family:'LiberationSerif';font-size:11.25pt;color:#000000"> will correctly see that an</span>
<span style="font-family:'LiberationSerif';font-size:11.25pt;color:#000000">interruption occurred.</span>
<span style="font-family:'LiberationSerif';font-size:11.25pt;color:#000000">We can wrap the </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">InterruptedException</span><span style="font-family:'LiberationSerif';font-size:11.25pt;color:#000000"> in a </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">RuntimeException</span><span style="font-family:'LiberationSerif';font-size:11.25pt;color:#000000">, which then</span>
<span style="font-family:'LiberationSerif';font-size:11.25pt;color:#000000">allows us the interruption to propagate up the call stack even through methods that don’t</span>
<span style="font-family:'LiberationSerif';font-size:11.25pt;color:#000000">declare </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">InterruptedException</span><span style="font-family:'LiberationSerif';font-size:11.25pt;color:#000000"> in their throws clause. This is particularly useful in</span>
<span style="font-family:'LiberationSerif';font-size:11.25pt;color:#000000">contexts where you cannot add checked exceptions to method signatures (such as when</span>
<span style="font-family:'LiberationSerif';font-size:11.25pt;color:#000000">implementing interfaces that don’t declare them).</span>

<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">For comparison, we will also keep our original sequential version in the</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">code:</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">//</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#35586c"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">Sequential</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#35586c"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">version</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#35586c"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">(original)</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">public Credit calculateCredit(Long personId) {</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">//</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#35586c"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">Method</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#35586c"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">body</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#35586c"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">available</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#35586c"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">in</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#35586c"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">the</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#35586c"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">above.</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#35586c"> </span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">}</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">To analyze performance improvements, it’s helpful to measure the</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">execution time of methods. Here’s a utility class for this purpose:</span>

---

## Page 40

<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">import java.util.concurrent.Callable;</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">public class ExecutionTimer {</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">public static &lt;T&gt; T measure(Callable&lt;T&gt; task) throws Exception {</span>

<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">long startTime = System.nanoTime();</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">try {</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">return task.call();</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">} finally {</span>

<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">long endTime = System.nanoTime();</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">//</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#35586c"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">Convert</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#35586c"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">to</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#35586c"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">milliseconds</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">long duration = (endTime - startTime) / 1_000_000;</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">System.out.println(&quot;Execution</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#cc3300"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">time:</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#cc3300"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">&quot; + duration + &quot;</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">milliseconds&quot;);</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">}</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">}</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">}</span>

<span style="font-family:'LiberationSans-Bold';font-size:14.99pt;font-weight:bold;color:#737373">TIP</span>

<span style="font-family:'LiberationSerif';font-size:11.25pt;color:#000000">This is a simplistic approach for understanding code execution. For robust</span>
<span style="font-family:'LiberationSerif';font-size:11.25pt;color:#000000">benchmarking, consider using a microbenchmark harness (like </span><span style="font-family:'LiberationSerif';font-size:11.25pt;color:#8e0012">JMH</span><span style="font-family:'LiberationSerif';font-size:11.25pt;color:#000000">) to account for</span>
<span style="font-family:'LiberationSerif';font-size:11.25pt;color:#000000">factors like JVM warmup, dead-code elimination, and other optimizations.</span>

<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Assuming that each method has a 200-millisecond execution time, we can</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">determine how much time the preceding code will require for completion.</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">The </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">Thread.sleep(200)</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000"> method can be used to simulate this execution</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">time. Now, let’s create a demonstration that compares both approaches:</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">public class ParallelExecutionDemo {</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">public static void main(String[] args) throws Exception {</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">CreditCalculatorService service = new</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">CreditCalculatorService();</span>

<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">System.out.println(&quot;===</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#cc3300"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">Sequential</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#cc3300"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">Execution</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#cc3300"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">===&quot;);</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">ExecutionTimer.measure(() -&gt; service.calculateCredit(1L));</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">System.out.println(&quot;\n===</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#cc3300"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">Parallel</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#cc3300"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">Execution</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#cc3300"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">===&quot;);</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">ExecutionTimer.measure(() -&gt; {</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">try {</span>

---

## Page 41

<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">return</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">service.calculateCreditWithUnboundedThreads(1L);</span>

<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">} catch (InterruptedException e) {</span>

<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">Thread.currentThread().interrupt();</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">throw new RuntimeException(e);</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">}</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">});</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">}</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">}</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">If we execute the preceding code, it will print something like this:</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">=== Sequential Execution ===</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">Important work completed</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">Execution time: 1026 milliseconds</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">=== Parallel Execution ===</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">Important work completed</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">Execution time: 616 milliseconds</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">We’ve significantly improved the performance by refactoring the credit</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">calculation and employing multiple threads. The new version runs 1.66</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">times faster than the old one (616 milliseconds compared to 1,026</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">milliseconds for the previous version), which is noticeably faster for the</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">user.</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Though parallelization enhances performance by mitigating or reducing the</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">blocking time of the initiating thread, it comes with its own challenges,</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">particularly concerning thread management and system resource utilization.</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">The ad hoc creation of threads, as demonstrated, lacks a controlled</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">mechanism for managing thread lifecycles and resource allocation. This can</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">lead to an overproliferation of threads, which has the potential to exhaust</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">system resources. Each thread, being a heavyweight resource, consumes</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">memory and processing power. As a result, since threads are created on an</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">ad hoc basis and there is insufficient control, threads will be created in</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">abundance, leading to </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">java.lang.OutOfMemoryError</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000"> exceptions,</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">crashes, and other runtime bottlenecks.</span>
<span style="font-family:'LiberationSans-Bold';font-size:16.88pt;font-weight:bold;color:#000000">Introducing the Executor Framework</span>

---

## Page 42

<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">To avoid the dangers of creating ad hoc threads, it’s better to use one of the</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Java concurrency frameworks, such as the </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">ExecutorService</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">, that</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">provides a more structured way of working with threads.</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Furthermore, using an </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">ExecutorService</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000"> not only controls the number of</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">concurrent running threads but also allows for efficient reuse of threads by</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">managing their life­cycle for us, thus overcoming the overhead brought by</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">the thread lifecycle.</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Let’s refactor the previous code and use </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">ExecutorService:</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">public Credit calculateCreditWithExecutor(Long personId)</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">throws ExecutionException, InterruptedException {</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">try (ExecutorService executor = Executors.newFixedThreadPool(5)) {</span>

<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">var person = getPerson(personId);</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">var assetsFuture = executor.submit(() -&gt; getAssets(person));</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">var liabilitiesFuture = executor.submit(() -&gt;</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">getLiabilities(person));</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">executor.submit(() -&gt; importantWork());</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">return calculateCredits(assetsFuture.get(),</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">liabilitiesFuture.get());</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">}</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">}</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Let’s walk through what we have done here:</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">Creates a thread pool that will be automatically shut down</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">when the </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">try</span><span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000"> block exits</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">Submits both critical tasks that we need results from</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">Submits additional work that doesn’t need to block the main</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">flow</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">Waits for the critical tasks and processes their results</span>

![](./_Modern_Concurrency_in_Java_-_A_N_M_Bazlur_Rahman_assets/images/p042_img01.png)



![](./_Modern_Concurrency_in_Java_-_A_N_M_Bazlur_Rahman_assets/images/p042_img02.png)



![](./_Modern_Concurrency_in_Java_-_A_N_M_Bazlur_Rahman_assets/images/p042_img03.png)



![](./_Modern_Concurrency_in_Java_-_A_N_M_Bazlur_Rahman_assets/images/p042_img04.png)



![](./_Modern_Concurrency_in_Java_-_A_N_M_Bazlur_Rahman_assets/images/p042_img05.png)



![](./_Modern_Concurrency_in_Java_-_A_N_M_Bazlur_Rahman_assets/images/p042_img06.png)



![](./_Modern_Concurrency_in_Java_-_A_N_M_Bazlur_Rahman_assets/images/p042_img07.png)



![](./_Modern_Concurrency_in_Java_-_A_N_M_Bazlur_Rahman_assets/images/p042_img08.png)



![](./_Modern_Concurrency_in_Java_-_A_N_M_Bazlur_Rahman_assets/images/p042_img09.png)

---

## Page 43

<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Now, let’s measure the execution time of the code using</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">ExecutionTimer.measure()</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000"> and compare the results to the previous</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">implementation:</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">public static void main(String[] args) throws Exception {</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">System.out.println(&quot;\n===</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#cc3300"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">Parallel</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#cc3300"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">Execution</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#cc3300"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">With</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#cc3300"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">Executors</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#cc3300"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">===&quot;);</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">ExecutionTimer.measure(() -&gt; {</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">try {</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">return service.calculateCreditWithExecutor(1L);</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">} catch (InterruptedException e) {</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">Thread.currentThread().interrupt();</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">throw new RuntimeException(e);</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">}</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">});</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">}</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">If we run the code, we will get output as follows:</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">=== Parallel Execution With Executors ===</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">Important work completed</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">Execution time: 630 milliseconds</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Interestingly, the execution time remains very similar to the previous</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">version. The executors provide useful facilities for managing concurrent</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">work in Java applications—they make it easier to create and manage</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">threads, they provide possible speedups for suitable workloads, and they</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">make the code easier to organize and keep clean. Now, let’s address the</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">outstanding obstacles of the Executors package.</span>
<span style="font-family:'LiberationSans-Bold';font-size:16.88pt;font-weight:bold;color:#000000">Remaining Challenges</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">The Executor framework brings significant improvements in resource</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">management and asynchronous execution to Java applications. However, to</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">maximize its benefits, it’s crucial to be aware of its limitations.</span>
<span style="font-family:'LiberationSerif-Italic';font-size:15.00pt;font-style:italic;color:#000000">Blocking on </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">Future.get()</span>

---

## Page 44

<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">Despite introducing asynchrony via </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">Future</span><span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000"> objects, the</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">get()</span><span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000"> method call still blocks execution. This means that</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">while we’ve shifted the blocking from one place to another,</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">we haven’t completely eliminated it.</span>
<span style="font-family:'LiberationSerif-Italic';font-size:15.00pt;font-style:italic;color:#000000">Potential for cache coherence overhead and false sharing</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">When tasks are submitted to a thread pool, they are</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">executed by threads that may run on different CPU cores</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">than the main thread. This could lead to scenarios where the</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">cache states of the two cores become inconsistent, creating</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">the potential for performance degradation due to cache</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">coherence protocols constantly invalidating and</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">synchronizing cache lines across cores.</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">The idea is to improve data access speed so all modern CPUs</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">have L-caches.</span><span style="font-family:'LiberationSans';font-size:11.25pt;color:#8e0012">3</span><span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000"> These caches store small chunks of data (size</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">varies with hardware architecture) so that the next time the</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">CPU needs that data, it can be retrieved from the cache</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">instead of from slower main memory. This chunk of data is</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">called a </span><span style="font-family:'NotoSerif-Italic';font-size:15.00pt;font-style:italic;color:#000000">cache line</span><span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">, and its use significantly improves overall</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">performance.</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">However, when multiple tasks run concurrently, there’s a</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">chance that data from two subsequent tasks might reside in</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">the same cache line. If both tasks run on the same CPU,</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">there’s no need to look up the main memory again, as the</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">data is already cached. That means the second operation will</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">have been faster than the first, which is a small</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">improvement. However, if the two tasks run on different</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">CPUs, the second CPU has to invalidate its cache line if the</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">first CPU modified any part of it, then reload the entire cache</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">line from main memory or another core’s cache.</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">This phenomenon, known as false sharing, occurs when</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">different threads modify different variables that happen to</span>

---

## Page 45

<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">share the same cache line. The frequent cache line</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">invalidations and reloads may negatively impact overall</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">performance in frameworks like the Executor framework,</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">where tasks are distributed across CPUs.</span>
<span style="font-family:'LiberationSerif-Italic';font-size:15.00pt;font-style:italic;color:#000000">Lack of composability</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">The Executor framework is still quite imperative in nature.</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">There is nothing wrong with imperative-style code, but</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">many developers find functional and declarative styles to be</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">easier to read and maintain.</span>

<span style="font-family:'LiberationSans-Bold';font-size:14.99pt;font-weight:bold;color:#737373">CACHE COHERENCE PROTOCOLS</span>

<span style="font-family:'LiberationSerif';font-size:11.25pt;color:#000000">Modern CPUs use cache coherence protocols (e.g., MESI, MOESI) to ensure all cores</span>
<span style="font-family:'LiberationSerif';font-size:11.25pt;color:#000000">see a consistent view of memory. When one core writes to a cache line, the protocol</span>
<span style="font-family:'LiberationSerif';font-size:11.25pt;color:#000000">invalidates or updates that line in other cores’ caches. This guarantees correctness but</span>
<span style="font-family:'LiberationSerif';font-size:11.25pt;color:#000000">can introduce performance overhead due to frequent invalidations and data transfers</span>
<span style="font-family:'LiberationSerif';font-size:11.25pt;color:#000000">across cores.</span>

<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">This leads back to our starting point: the natural question is “What now?”</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">How do we solve these problems, and how can we use executors to write</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">even more efficient and maintainable code in the future?</span>
<span style="font-family:'LiberationSans-Bold';font-size:21.25pt;font-weight:bold;color:#8e0012">Beyond Basic Thread Pools</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">The Executor framework is very helpful; however, scenarios like heavy</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">cache contention or the need for complex task dependencies can be</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">problematic, as I stated previously. Java’s Fork/Join Pool addresses these</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">challenges with a performance-focused design and specialized algorithms.</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">This dedicated implementation of </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">Executor</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">​</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">Ser</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">⁠</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">vice</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000"> delivers a more</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">sophisticated thread-pooling mechanism, enhancing performance and</span>

---

## Page 46

<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">resource efficiency through intelligent task scheduling. Let’s explore</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">further.</span>
<span style="font-family:'LiberationSans-Bold';font-size:16.88pt;font-weight:bold;color:#000000">Cache Affinity and Task Distribution</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Cache affinity refers to the concept of completing tasks in a way that</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">leverages the locality of the CPU cache. If a task is executed on a particular</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">CPU, its data gets loaded into the cache of that core. If other related tasks</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">are executed on the same core, those other tasks can benefit from the data</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">already cached there, reducing the memory access time and, thus,</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">improving performance.</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">The Fork/Join pool encourages cache affinity because worker threads</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">usually execute tasks from their own local queues. This is especially helpful</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">if the tasks frequently access the same shared data. By maintaining cache</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">affinity, the Fork/Join Pool minimizes cache misses, which occur when the</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">data needed by a task is not present in the cache and must be fetched from</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">the main memory—a significantly slower process.</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Consider, for instance, a set of parallel tasks working on a large array. If</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">these tasks are all processing different slices of that array, it pays to have</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">them run on the same core; then, the data in those slices of an array can stay</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">in the cache and be accessed quickly by the next task. Otherwise, we will</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">have to continually reload this data into the cache. Cache affinity reduces</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">the overhead associated with repeatedly loading data into the cache.</span>
<span style="font-family:'LiberationSans-Bold';font-size:16.88pt;font-weight:bold;color:#000000">Work-Stealing Algorithm</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">The Fork/Join Pool also employs a dynamic work-stealing algorithm to</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">efficiently balance the load among threads. Instead of a single shared task</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">queue, each thread in a Fork/Join Pool has its own queue. When a thread</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">runs out of work, it “steals” tasks from the tail of another thread’s queue.</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">This dynamic work-stealing algorithm maximizes CPU usage, ensuring that</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">no thread sits idle while there are tasks to be done. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#8e0012">Figure 1-3</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000"> illustrates the</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">work-stealing algorithm used in the Fork/Join Pool. This method of</span>

---

## Page 47

<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">dynamically redistributing tasks ensures that all threads remain productive,</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">enhancing overall CPU utilization and minimizing idle time.</span>

---

## Page 48

Fork/join pool
Thread1
Task A

Thread1  Queue1  Task B

TaskC
:Steals task A

Thread2  Thread3
Thread1  Thread3  Queue3
empty

Queue1

Task D

<!-- 图源: ./_Modern_Concurrency_in_Java_-_A_N_M_Bazlur_Rahman_assets/images/p048_scan.jpg -->

---

## Page 49

<span style="font-family:'LiberationSerif-Italic';font-size:11.25pt;font-style:italic;color:#000000">Figure 1-3. Work-stealing algorithm in Fork/Join Pool</span>

<span style="font-family:'LiberationSans-Bold';font-size:15.00pt;font-weight:bold;color:#555555">Example of using a Fork/Join Pool</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">The Fork/Join Pool simplifies the process of sending tasks, and it</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">automatically distributes the tasks efficiently without requiring additional</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">management. Here’s a basic example of how to use the Fork/Join Pool:</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">ForkJoinPool forkJoinPool = new ForkJoinPool();</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">forkJoinPool.submit(() -&gt; {</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">//</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#35586c"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">your</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#35586c"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">parallelized</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#35586c"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">tasks</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#35586c"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">here</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">}).join();</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Notice how easy it is to submit tasks to a Fork/Join Pool. The framework</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">automatically handles efficient work distribution, taking the burden of</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">complex thread management off the developer.</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Typically, the Fork/Join Pool is associated with the divide-and-conquer</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">algorithm, using the </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">RecursiveAction</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000"> and </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">RecursiveTask</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000"> classes to</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">break tasks into smaller pieces for the Fork/Join Pool. However, in this</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">context, we are focusing on the Fork/Join Pool itself, not the divide-and-</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">conquer strategies used to create these tasks.</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">I’ll discuss the Fork/Join Pool in more detail in the following chapter.</span>
<span style="font-family:'LiberationSans-Bold';font-size:16.88pt;font-weight:bold;color:#000000">Bringing Composability into Play with</span>
<span style="font-family:'LiberationSans-Bold';font-size:16.88pt;font-weight:bold;color:#000000">CompletableFuture</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">The Executor framework and the Fork/Join Pool offer performance</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">improvements, but composing complex asynchronous workflows can still</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">be cumbersome. To address this challenge, Java 8 introduced</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">CompletableFuture</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">, a class designed to streamline asynchronous</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">programming by focusing on composability and ease of use.</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Let’s look at how a composable and fluent API with</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">CompletableFuture</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000"> aligns with the earlier example.</span>

---

## Page 50

<span style="font-family:'LiberationSans-Bold';font-size:15.00pt;font-weight:bold;color:#555555">Composable and fluent API</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">CompletableFuture</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000"> transforms how we write asynchronous code,</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">moving away from callback-heavy styles toward a declarative and</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">functional approach. Its rich API empowers us to chain multiple</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">asynchronous operations effortlessly, improving code readability and</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">maintainability. Let’s revisit the previous example:</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">import java.util.concurrent.CompletableFuture;</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">import java.util.concurrent.ExecutionException;</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">import</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#006699"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">static java.util.concurrent.CompletableFuture.*;</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">public Credit calculateCreditWithCompletableFuture(Long personId)</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">throws InterruptedException, ExecutionException {</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">return runAsync(() -&gt; importantWork())</span>

<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">.thenCompose(aVoid -&gt; supplyAsync(() -&gt; getPerson(personId)))</span>

<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">.thenCombineAsync(supplyAsync(() -&gt;</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">getAssets(getPerson(personId))),</span>

<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">(person, assets)</span>

<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">-&gt; calculateCredits(assets,</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">getLiabilities(person)))</span>

<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">.get();</span>

<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">}</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Let’s examine how this </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">CompletableFuture</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000"> approach works:</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">Starts the independent work asynchronously</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">After important work completes, begins fetching </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">person</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">data asynchronously</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">Combines the </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">person</span><span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000"> data with </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">assets</span><span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000"> fetched in parallel</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">Calculates the final credit score using the combined results</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">Blocks and waits for the final result to complete</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">This example demonstrates how </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">CompletableFuture</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000"> allows us to</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">structure asynchronous workflows in a clear and concise manner. However,</span>

![](./_Modern_Concurrency_in_Java_-_A_N_M_Bazlur_Rahman_assets/images/p050_img01.png)



![](./_Modern_Concurrency_in_Java_-_A_N_M_Bazlur_Rahman_assets/images/p050_img02.png)



![](./_Modern_Concurrency_in_Java_-_A_N_M_Bazlur_Rahman_assets/images/p050_img03.png)



![](./_Modern_Concurrency_in_Java_-_A_N_M_Bazlur_Rahman_assets/images/p050_img04.png)



![](./_Modern_Concurrency_in_Java_-_A_N_M_Bazlur_Rahman_assets/images/p050_img05.png)



![](./_Modern_Concurrency_in_Java_-_A_N_M_Bazlur_Rahman_assets/images/p050_img06.png)



![](./_Modern_Concurrency_in_Java_-_A_N_M_Bazlur_Rahman_assets/images/p050_img07.png)



![](./_Modern_Concurrency_in_Java_-_A_N_M_Bazlur_Rahman_assets/images/p050_img08.png)



![](./_Modern_Concurrency_in_Java_-_A_N_M_Bazlur_Rahman_assets/images/p050_img09.png)



![](./_Modern_Concurrency_in_Java_-_A_N_M_Bazlur_Rahman_assets/images/p050_img10.png)

---

## Page 51

<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">it is not without its disadvantages.</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Let’s explore the advantages and disadvantages of this API.</span>
<span style="font-family:'LiberationSans-Bold';font-size:15.00pt;font-weight:bold;color:#555555">Advantages of using CompletableFuture</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">CompletableFuture</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000"> transforms how you structure asynchronous code</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">with its fluent API. Instead of tangled nested callbacks or sprawling class</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">hierarchies of task objects, you define workflows as a series of</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">transformations and intermediate computations. This improves readability</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">and maintainability, reducing the likelihood of bugs in applications where</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">asynchrony is central.</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Under the hood, </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">CompletableFuture</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000"> builds on the Fork/Join</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">framework, leveraging optimized work-stealing for efficient task</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">distribution and better scalability under load. Built-in methods such as</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">exceptionally, handle, and </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">whenComplete</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000"> give you explicit control over</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">error handling. You can also supply custom executors to fine-tune thread</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">management for specific workloads. Finally, while a blocking </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">get()</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000"> is</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">sometimes required at boundaries, the majority of your pipeline can remain</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">non-blocking, enabling highly responsive and resource-efficient code.</span>
<span style="font-family:'LiberationSans-Bold';font-size:15.00pt;font-weight:bold;color:#555555">Disadvantages and limitations</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Although </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">CompletableFuture</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000"> is a powerful feature in Java, it does</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">require you to shift your mindset if you are already used to the imperative</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">programming style. I would say that you may have to invest quite a bit to</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">learn this rich API in order to use it in a meaningful way. You have to figure</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">out the best places or value propagation techniques for the various methods</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">it offers. This would be a considerable investment of time and practice, and</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">with that comes the possibility of failing to use it correctly.</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">For example, the </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">get()</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000"> method is still a blocking method, which is</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">necessary but can induce blocking in a flow that would otherwise</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">undermine the use of asynchronous programming altogether if not used</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">frugally. You also can have an issue in multichain </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">CompletableFuture</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">cases where you have more than one dependency and have to propagate and</span>

---

## Page 52

<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">check the error in order to recover. This, if not designed correctly, can</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">create a very big nightmare to debug. Finally, debugging code written in</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">CompletableFuture</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">’s chained style poses another challenge in flow</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">control. If you are used to the default debugging tools available in various</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">IDEs, stepping through the code, setting a breakpoint at a particular spot,</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">and seeing the context of a statement by taking a step backward in the code</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">is going to be highly challenging for you. This is because of the inverted</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">and ambiguous execution flow of asynchronous code—it doesn’t just get</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">executed line by line.</span>

<span style="font-family:'LiberationSans-Bold';font-size:14.99pt;font-weight:bold;color:#737373">NOTE</span>

<span style="font-family:'LiberationSerif';font-size:11.25pt;color:#000000">While the benefits of asynchronous programming are clear, and </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">CompletableFuture</span>
<span style="font-family:'LiberationSerif';font-size:11.25pt;color:#000000">provides powerful features along with other alternatives like reactive frameworks, we</span>
<span style="font-family:'LiberationSerif';font-size:11.25pt;color:#000000">must ask a critical question: Are we organizationally ready for this complexity?</span>
<span style="font-family:'LiberationSerif';font-size:11.25pt;color:#000000">Asynchronous programming fundamentally changes how we design, debug, and</span>
<span style="font-family:'LiberationSerif';font-size:11.25pt;color:#000000">maintain applications. The gains in performance come with significant costs in cognitive</span>
<span style="font-family:'LiberationSerif';font-size:11.25pt;color:#000000">overhead, debugging difficulty, and architectural complexity. Before adopting these</span>
<span style="font-family:'LiberationSerif';font-size:11.25pt;color:#000000">patterns, consider whether your team has the experience to maintain clean code</span>
<span style="font-family:'LiberationSerif';font-size:11.25pt;color:#000000">principles, handle complex error scenarios, and debug asynchronous call chains</span>
<span style="font-family:'LiberationSerif';font-size:11.25pt;color:#000000">effectively.</span>
<span style="font-family:'LiberationSerif';font-size:11.25pt;color:#000000">This isn’t merely a technical decision. It’s an architectural commitment that affects</span>
<span style="font-family:'LiberationSerif';font-size:11.25pt;color:#000000">every aspect of your application’s lifecycle. Choose wisely based on your team’s</span>
<span style="font-family:'LiberationSerif';font-size:11.25pt;color:#000000">capabilities and your application’s actual performance requirements.</span>

<span style="font-family:'LiberationSans-Bold';font-size:21.25pt;font-weight:bold;color:#8e0012">A Different Paradigm for Asynchronous</span>
<span style="font-family:'LiberationSans-Bold';font-size:21.25pt;font-weight:bold;color:#8e0012">Programming</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Although </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">CompletableFuture</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000"> provides powerful tools, changing one’s</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">mindset is not enough to address the remaining limitations (or to reach the</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">higher levels of performance achievable in some cases). Reactive</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">programming introduces a paradigm focused on data streams, asynchronous</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">event processing, and non-blocking operations. Frameworks like RxJava,</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Akka, Eclipse Vert.x, Spring WebFlux, and others implement this paradigm</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">in Java with rich toolsets.</span>

---

## Page 53

<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">For instance, let’s reimagine our previous </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">calculateCredit()</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000"> example</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">using Spring WebFlux. First, we need to add the necessary dependency and</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">imports:</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">&lt;dependency&gt;</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">&lt;groupId&gt;org.springframework&lt;/groupId&gt;</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">&lt;artifactId&gt;spring-webflux&lt;/artifactId&gt;</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">&lt;version&gt;6.0.0&lt;/version&gt;</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">&lt;/dependency&gt;</span>

<span style="font-family:'LiberationSans-Bold';font-size:14.99pt;font-weight:bold;color:#737373">TIP</span>

<span style="font-family:'LiberationSerif';font-size:11.25pt;color:#000000">To test the reactive example, create a Maven project and add the </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">reactor-core</span>
<span style="font-family:'LiberationSerif';font-size:11.25pt;color:#000000">dependency, as shown in the preceding </span><span style="font-family:'LiberationSerif-Italic';font-size:11.25pt;font-style:italic;color:#000000">pom.xml</span><span style="font-family:'LiberationSerif';font-size:11.25pt;color:#000000">.</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Now we’ll add the following method to our</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">CreditCalculatorService.java</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000"> class:</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">public Mono&lt;Credit&gt; calculateCreditReactive(Long personId) {</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">Mono&lt;Void&gt; importantWorkMono = Mono.fromRunnable(() -&gt;</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">importantWork());</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">Mono&lt;Person&gt; personMono = Mono.fromSupplier(() -&gt;</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">getPerson(personId));</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">Mono&lt;List&lt;Asset&gt;&gt; assetsMono = personMono</span>

<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">.map(person -&gt; getAssets(person));</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">Mono&lt;List&lt;Liability&gt;&gt; liabilitiesMono = personMono</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">.map(person -&gt; getLiabilities(person));</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">return importantWorkMono.then(</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">Mono.zip(assetsMono, liabilitiesMono)</span>

<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">.map(tuple -&gt; {</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">List&lt;Asset&gt; assets = tuple.getT1();</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">List&lt;Liability&gt; liabilities = tuple.getT2();</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">return calculateCredits(assets, liabilities);</span>

<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">})</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">);</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">}</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Let’s examine the reactive flow:</span>

![](./_Modern_Concurrency_in_Java_-_A_N_M_Bazlur_Rahman_assets/images/p053_img01.png)



![](./_Modern_Concurrency_in_Java_-_A_N_M_Bazlur_Rahman_assets/images/p053_img02.png)



![](./_Modern_Concurrency_in_Java_-_A_N_M_Bazlur_Rahman_assets/images/p053_img03.png)



![](./_Modern_Concurrency_in_Java_-_A_N_M_Bazlur_Rahman_assets/images/p053_img04.png)



![](./_Modern_Concurrency_in_Java_-_A_N_M_Bazlur_Rahman_assets/images/p053_img05.png)



![](./_Modern_Concurrency_in_Java_-_A_N_M_Bazlur_Rahman_assets/images/p053_img06.png)



![](./_Modern_Concurrency_in_Java_-_A_N_M_Bazlur_Rahman_assets/images/p053_img07.png)

---

## Page 54

<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">Creates a </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">Mono</span><span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000"> that executes important work</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">asynchronously when subscribed</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">Wraps the </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">person</span><span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000"> lookup in a reactive stream</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">Transforms the </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">person</span><span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000"> stream to fetch assets</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">asynchronously</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">Transforms the </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">person</span><span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000"> stream to fetch liabilities</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">asynchronously</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">Waits for important work to complete, then proceeds with</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">credit calculation</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">Combines </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">assets</span><span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000"> and </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">liabilities</span><span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000"> streams into a single</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">stream containing both results</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">Calculates the final credit score from the combined data</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Looking at this code, you’ll notice it’s conceptually similar to the</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">CompletableFuture</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000"> approach but uses reactive streams terminology and</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">operators. However, if you’re unfamiliar with reactive programming, this</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">approach can be puzzling and requires significant learning investment to</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">understand concepts like Publishers, Subscribers, backpressure, and</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">reactive operators.</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">I will not explain reactive programming in this book, as this is not the goal</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">of this book, but you can explore other books written about the reactive</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">stack.</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Let’s discuss some of the limitations we encountered in the reactive</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">programming approach just to make sense of why we need something</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">different or why we need virtual threads, which </span><span style="font-family:'LiberationSerif-Italic';font-size:15.00pt;font-style:italic;color:#000000">is</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000"> the goal of this book.</span>
<span style="font-family:'LiberationSans-Bold';font-size:16.88pt;font-weight:bold;color:#000000">Drawbacks of Using Reactive Frameworks</span>

![](./_Modern_Concurrency_in_Java_-_A_N_M_Bazlur_Rahman_assets/images/p054_img01.png)



![](./_Modern_Concurrency_in_Java_-_A_N_M_Bazlur_Rahman_assets/images/p054_img02.png)



![](./_Modern_Concurrency_in_Java_-_A_N_M_Bazlur_Rahman_assets/images/p054_img03.png)



![](./_Modern_Concurrency_in_Java_-_A_N_M_Bazlur_Rahman_assets/images/p054_img04.png)



![](./_Modern_Concurrency_in_Java_-_A_N_M_Bazlur_Rahman_assets/images/p054_img05.png)



![](./_Modern_Concurrency_in_Java_-_A_N_M_Bazlur_Rahman_assets/images/p054_img06.png)



![](./_Modern_Concurrency_in_Java_-_A_N_M_Bazlur_Rahman_assets/images/p054_img07.png)

---

## Page 55

<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">As the saying goes, “There’s no such thing as a free lunch,” and the same</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">applies to reactive programming. Let’s outline some of the key challenges it</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">presents:</span>
<span style="font-family:'LiberationSerif-Italic';font-size:15.00pt;font-style:italic;color:#000000">Steep learning curve</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">Grasping the fundamentals of the reactive programming</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">paradigm requires a significant mental shift for developers</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">who are already used to the imperative programming</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">paradigm. Concepts like </span><span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#8e0012">Observables</span><span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">, </span><span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#8e0012">Observable operators</span><span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">,</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#8e0012">schedulers</span><span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">, and </span><span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#8e0012">backpressure</span><span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">, which are fundamental to</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">reactive frameworks, require an investment of time and a</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">potentially steep learning curve compared to other</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">concurrency models.</span>
<span style="font-family:'LiberationSerif-Italic';font-size:15.00pt;font-style:italic;color:#000000">Increased cognitive load</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">The functional programming style used in reactive</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">frameworks can be too much for full-time developers with</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">long tenures of imperative programming or object-oriented</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">programming under their belts. Along with the heavy usage</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">of lambdas, higher-order functions, and functional</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">composition, chains of operators and transformations can</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">make the code itself harder to grasp initially and, thus, more</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">challenging to maintain (at least in the short term) among</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">large projects or teams where not all the members have a</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">similar amount of reactive programming experience.</span>
<span style="font-family:'LiberationSerif-Italic';font-size:15.00pt;font-style:italic;color:#000000">Debugging difficulties</span>

---

## Page 56

<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">Traditional debugging tools and techniques don’t always</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">work so well with reactive systems on the fly. For instance,</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">chained operators lead to asynchronous execution where, if</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">an error does arise, the stack trace might provide</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">insufficient context to find the root of the problem. You</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">might miss the event that last occurred through event</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">hopping between threads and operators. Specialized</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">debugging tools, possibly provided as part of your reactive</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">framework, might be required. This adds to the learning</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">curve. For example, suppose a reactive pipeline fetches data</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">from four different sources, transforms the four streams,</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">and then combines the results. An error arising from the</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">combine phase might result in a stack trace that points to the</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">combine operator and makes it difficult to tell whether a</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">particular upstream source or transformation is the root</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">cause of the problem. Tracking the data flow throughout the</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">reactive pipeline becomes tricky in complex pipelines with</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">dozens of operators, tens of asynchronous operations, and so</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">on. In the traditional threading model, debugging is</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">relatively straightforward and comes at little cost.</span>
<span style="font-family:'LiberationSerif-Italic';font-size:15.00pt;font-style:italic;color:#000000">Overcomplication risk</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">The composability possible through reactive frameworks’</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">operator-based models, while very powerful, comes with the</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">potential to create more complex products than are</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">absolutely necessary for a given business requirement or</span>

---

## Page 57

<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">user interface element. Just as one might be able to cook a</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">chicken curry using every dish in a family’s silver set, it is</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">possible (and tempting) to solve every problem with</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">something slightly more complicated than is really</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">necessary. As such, reactive frameworks face the creative</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">risk of allowing inexperienced programmers to</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">overengineer their domain—a risk not unique to reactive</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">frameworks themselves but one that occurs in any powerful</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">abstraction or library when used by those who do not know</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">when to stop.</span>
<span style="font-family:'LiberationSerif-Italic';font-size:15.00pt;font-style:italic;color:#000000">Potential mismatch</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">Reactive programming frameworks are best suited for</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">scenarios involving significant asynchronous operations,</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">event-driven data flows, or high-throughput data streams. In</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">other situations, some subset of reactive systems’ or</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">frameworks’ capabilities might not be needed because there</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">isn’t much asynchronicity or the data flow isn’t inherently</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">event-oriented, or the effort to learn, abstract, and apply into</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">the framework justifies the greater simplicity of a more</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">basic synchronous or request-response approach.</span>
<span style="font-family:'LiberationSerif-Italic';font-size:15.00pt;font-style:italic;color:#000000">Vendor lock-in</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">The essential ideas of reactive programming are</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">undoubtedly transferable; however, each of the significant</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">reactive frameworks has its own set of nuanced APIs.</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">Picking one of them over the other means you will be locked</span>

---

## Page 58

<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">into a specific framework, which naturally makes it harder</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">to change libraries later, and potentially constrains flexibility</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">somewhere down the line of the project’s lifecycle. This lock-</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">in can also have implications, such as difficulty in finding</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">developers familiar with the chosen framework, potential</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">for framework stagnation or lack of continued support, and</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">challenges in migrating to a different framework if needed.</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Having mentioned all of these challenges, there is absolutely no denying</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">that reactive programming frameworks have the potential to enormously</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">simplify the management of sophisticated asynchronous scenarios.</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">However, we must examine their trade-offs in detail to understand if the</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">benefits outweigh the costs for the project we’re working on.</span>
<span style="font-family:'LiberationSans-Bold';font-size:21.25pt;font-weight:bold;color:#8e0012">Revolutionizing Concurrency in Java</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Java’s traditional concurrency mechanisms have served us well, but as the</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">application of concurrency has grown to more complex and additional use</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">cases, various challenges have been presented along the way. That’s why it</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">is essential to explore new solutions.</span>
<span style="font-family:'LiberationSans-Bold';font-size:16.88pt;font-weight:bold;color:#000000">The Promise of Virtual Threads</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Now that we understand the shortcomings of our traditional approaches, we</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">need to find a better way to fix those problems. This is precisely where</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Project Loom, a key step toward first-class modern concurrency in Java,</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">comes into play. It introduced a new kind of thread called </span><span style="font-family:'LiberationSerif-Italic';font-size:15.00pt;font-style:italic;color:#000000">virtual threads</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">,</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">ushering in a new era of concurrency in Java. These are very lightweight</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">threads and can be created on demand with next to no overhead compared</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">to what is traditionally associated with thread creation. Virtual threads can</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">be instantiated in the millions, unlocking new possibilities for highly</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">concurrent and scalable applications.</span>

---

## Page 59

<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Let’s discuss some of the characteristics of virtual threads.</span>
<span style="font-family:'LiberationSans-Bold';font-size:16.88pt;font-weight:bold;color:#000000">Seamless Integration with Existing Codebases</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">One of the key strengths of Project Loom lies in its seamless compatibility</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">with existing Java codebases. For instance, if your application already uses</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">the Executor framework, all you have to do to take advantage of virtual</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">threads is pass an</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">Executors.newVirtualThreadPerTaskExecutor()</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000"> to your</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">existing </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">ExecutorService</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">:</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">Credit calculateCreditWithVirtualThread</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">(Long personId) throws ExecutionException, InterruptedException</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">{</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">var person = getPerson(personId);</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">var assetsFuture = executor.submit(() -&gt; getAssets(person));</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">var liabilitiesFuture = executor.submit(() -&gt;</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">getLiabilities(person));</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">executor.submit(this::importantWork);</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">return calculateCredits(assetsFuture.get(),</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">liabilitiesFuture.get());</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">}</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">}</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">Replace any traditional executor with</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">Executors.newVirtualThreadPerTask</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">​</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">Executor()</span><span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000"> to</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">immediately gain the benefits of virtual threads.</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">This ease of integration ensures a smooth transition to the new concurrency</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">model.</span>
<span style="font-family:'LiberationSans-Bold';font-size:16.88pt;font-weight:bold;color:#000000">Virtual Threads and Platform Threads</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Virtual threads ride on top of </span><span style="font-family:'LiberationSerif-Italic';font-size:15.00pt;font-style:italic;color:#000000">platform threads</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">, also known as </span><span style="font-family:'LiberationSerif-Italic';font-size:15.00pt;font-style:italic;color:#000000">classical</span>
<span style="font-family:'LiberationSerif-Italic';font-size:15.00pt;font-style:italic;color:#000000">threads</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">, which are also backed by the Fork/Join Pool, so they inherit the</span>

![](./_Modern_Concurrency_in_Java_-_A_N_M_Bazlur_Rahman_assets/images/p059_img01.png)



![](./_Modern_Concurrency_in_Java_-_A_N_M_Bazlur_Rahman_assets/images/p059_img02.png)

---

## Page 60

<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">benefits of that more sophisticated thread pool. Thus, virtual and platform</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">threads combine perfectly to produce the best resource utilization and</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">scalability results.</span>
<span style="font-family:'LiberationSans-Bold';font-size:16.88pt;font-weight:bold;color:#000000">Intelligent Handling of Blocking Operations</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">One of the most exciting aspects of virtual threads is their intelligent</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">handling of blocking operations. When a virtual thread encounters a</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">blocking operation—such as a sleep or network I/O—it automatically yields</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">control back to the underlying platform thread. This allows the platform</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">thread to continue executing other virtual threads, ensuring optimal</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">utilization of available resources. Once the blocking operation is complete,</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">the virtual thread can simply take up execution from where it left off,</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">avoiding perceptible bottlenecks.</span>
<span style="font-family:'LiberationSans-Bold';font-size:16.88pt;font-weight:bold;color:#000000">Benefits of Embracing Virtual Threads</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Before diving into the nitty-gritty of virtual threads in the next chapter, let’s</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">take a quick look at how virtual threads revolutionize the way we handle</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">concurrency in Java. Here are some of the key advantages:</span>
<span style="font-family:'LiberationSerif-Italic';font-size:15.00pt;font-style:italic;color:#000000">Resource efficiency</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">Virtual threads are lightweight, and we can spawn millions</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">without exhausting resources, which permits highly</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">concurrent, scalable applications.</span>
<span style="font-family:'LiberationSerif-Italic';font-size:15.00pt;font-style:italic;color:#000000">Code simplicity</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">With the ability to write blocking code without performance</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">penalties, developers can now write and maintain a more</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">straightforward, traditional imperative coding style. That</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">means no additional learning curve and easier-to-maintain</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">code.</span>

---

## Page 61

<span style="font-family:'LiberationSerif-Italic';font-size:15.00pt;font-style:italic;color:#000000">Optimal utilization</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">During blocking operations, virtual threads relinquish</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">control, which ensures that platform threads remain</span>
<span style="font-family:'NotoSerif-Regular';font-size:15.00pt;color:#000000">utilized, increasing CPU and application performance.</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">One of the biggest concerns for developers using concurrency in Java is the</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">specific information they must learn to use it correctly. Project Loom</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">represents a huge step forward for the concurrent style Java developers</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">create and use; it will let them write highly concurrent and efficient</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">applications at scale while still using the patterns they are already familiar</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">with.</span>
<span style="font-family:'LiberationSans-Bold';font-size:21.25pt;font-weight:bold;color:#8e0012">In Closing</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">In this book, we will continue discussing virtual threads and explore more</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">about this revolutionary technology in the next chapter.</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">As you dive deeper into virtual thread concepts, consider how they can</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">transform your approach to designing concurrent applications. Virtual</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">threads offer not just a new way to handle concurrency but also a more</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">efficient, scalable, and intuitive method that aligns with modern computing</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">demands.</span>
<span style="font-family:'LiberationSans';font-size:11.25pt;color:#8e0012">1</span><span style="font-family:'LiberationSerif';font-size:11.25pt;color:#000000">  Little’s Law is a key principle in queuing systems, including multithreaded applications. It</span>
<span style="font-family:'LiberationSerif';font-size:11.25pt;color:#000000">relates latency, concurrency, and throughput, which are crucial for computing performance. In</span>
<span style="font-family:'LiberationSerif';font-size:11.25pt;color:#000000">the next chapter, we’ll discuss this in detail and demonstrate how higher concurrency leads to</span>
<span style="font-family:'LiberationSerif';font-size:11.25pt;color:#000000">higher throughput.</span>
<span style="font-family:'LiberationSans';font-size:11.25pt;color:#8e0012">2</span><span style="font-family:'LiberationSerif';font-size:11.25pt;color:#000000">  This is the default size in most Linux environments, but it depends on the operating system</span>
<span style="font-family:'LiberationSerif';font-size:11.25pt;color:#000000">and can be tweaked.</span>
<span style="font-family:'LiberationSans';font-size:11.25pt;color:#8e0012">3</span><span style="font-family:'LiberationSerif';font-size:11.25pt;color:#000000">  L-caches are the tiered levels of superfast memory (L1, L2, L3) within a CPU that store</span>
<span style="font-family:'LiberationSerif';font-size:11.25pt;color:#000000">frequently used data for quick access.</span>

---

## Page 62

<span style="font-family:'LiberationSans-Bold';font-size:30.00pt;font-weight:bold;color:#000000">Chapter 2. Understanding</span>
<span style="font-family:'LiberationSans-Bold';font-size:30.00pt;font-weight:bold;color:#000000">Virtual Threads</span>

<span style="font-family:'LiberationSerif-Italic';font-size:15.00pt;font-style:italic;color:#000000">The price of reliability is the pursuit of the utmost simplicity. It is a price</span>
<span style="font-family:'LiberationSerif-Italic';font-size:15.00pt;font-style:italic;color:#000000">which the very rich find most hard to pay.</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Virtual threads are a groundbreaking addition to the Java concurrency</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">toolkit that are fundamentally changing how developers write concurrent</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">programs, making it practical to use threads as the primary unit of</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">concurrency at massive scale. As we will discuss at length in this chapter,</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">virtual threads differ significantly from the platform threads, or classical</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">threads, that have served us over the years. In particular, while platform</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">threads are managed by the underlying operating system or by thread</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">libraries like those from POSIX, virtual threads are lightweight threads</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">managed by the Java Virtual Machine (JVM) itself. This shift from OS-</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">managed to JVM-managed threads represents more than just an</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">implementation detail—it enables applications to create millions of threads</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">without the memory and performance penalties that made such designs</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">impractical with traditional threads. In this chapter, we’ll take a deep dive</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">into virtual threads by reviewing their architecture, discussing how they</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">differ from platform threads, examining the motivation behind their creation</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">for modern Java applications, and exploring how they simplify concurrent</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">programming while delivering unprecedented scalability.</span>
<span style="font-family:'LiberationSans-Bold';font-size:21.25pt;font-weight:bold;color:#8e0012">What Is a Virtual Thread?</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">Virtual threads are managed by the JVM, which allows them to operate</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">more efficiently than traditional threads that rely on the operating system</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">for scheduling and management. Virtual threads are executed on top of</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">carrier threads, which are essentially threads from the Fork/Join Pool. This</span>

<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">—Tony Hoare</span>

---

## Page 63

<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">design allows virtual threads to inherit the benefits of advanced thread</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">pooling mechanisms and efficient work-stealing algorithms.</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">It’s important to note that the virtual thread scheduler implemented within</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">the JVM is based on a work-stealing </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">ForkJoinPool</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">, operating in a First-</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">In, First-Out (FIFO) mode. This </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">ForkJoinPool</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000"> serves as the foundation</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">for scheduling virtual threads and is distinct from the common pool used for</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">other purposes, such as the implementation of parallel streams, which</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">operate in a Last-In, First-Out (LIFO) mode.</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">The parallelism of the virtual thread scheduler, which refers to the number</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">of platform threads available for scheduling virtual threads, is a</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">configurable parameter. By default, it is set to the number of available</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">processors on the system, ensuring optimal utilization of hardware</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">resources. However, developers can fine-tune this parameter using the</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">jdk.virtualThreadScheduler.parallelism</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000"> system property,</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">allowing them to adjust the level of parallelism based on their specific</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">application requirements and workload characteristics.</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">You can set system properties in Java using the </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">-D</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000"> command-line option</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">when you start your Java application, like this:</span>
<span style="font-family:'LiberationMono-Bold';font-size:11.25pt;font-weight:bold;color:#000088">java</span><span style="font-family:'LiberationMono-Bold';font-size:11.25pt;font-weight:bold;color:#bbbbbb"> </span><span style="font-family:'LiberationM