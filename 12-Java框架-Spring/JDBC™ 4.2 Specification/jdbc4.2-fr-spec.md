## Page 1

<span style="font-family:'Helvetica';font-size:6.95pt;color:#000000">March 2014  Final  v1.0</span>

<span style="font-family:'Palatino-Bold';font-size:13.91pt;font-weight:bold;color:#000000">JDBC</span><span style="font-family:'Palatino-Bold';font-size:11.15pt;font-weight:bold;color:#000000">™  </span><span style="font-family:'Palatino-Bold';font-size:13.91pt;font-weight:bold;color:#000000"> 4.2  Specification</span>

<span style="font-family:'Helvetica';font-size:15.94pt;color:#000000">JSR 221</span>

<span style="font-family:'Palatino-Bold';font-size:11.99pt;font-weight:bold;color:#000000">Lance Andersen, Specification Lead</span>
<span style="font-family:'Palatino-Roman';font-size:11.99pt;color:#000000">March 2014</span>

![](./jdbc4.2-fr-spec_assets/images/p001_img01.png)

---

## Page 2

<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">ORACLE AMERICA, INC. IS WILLING TO LICENSE THIS SPECIFICATION TO YOU ONLY UPON THE CONDITION THAT YOU </span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">ACCEPT ALL OF THE TERMS CONTAINED IN THIS AGREEMENT. PLEASE READ THE TERMS AND CONDITIONS OF THIS </span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">AGREEMENT CAREFULLY. BY DOWNLOADING THIS SPECIFICATION, YOU ACCEPT THE TERMS AND CONDITIONS OF THE </span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">AGREEMENT. IF YOU ARE NOT WILLING TO BE BOUND BY IT, SELECT THE &quot;DECLINE&quot; BUTTON AT THE BOTTOM OF THIS PAGE.</span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">Specification: JSR 221 JDBC API (&quot;Specification&quot;)</span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">Version: 4.2</span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">Status: Maintenance Release</span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">Specification Lead: Oracle America, Inc. (&quot;Specification Lead&quot;)</span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">Release: March 2014</span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">Copyright 2014 Oracle America, Inc.</span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">All rights reserved.</span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">LIMITED LICENSE GRANTS</span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">1. License for Evaluation Purposes. Specification Lead hereby grants you a fully-paid, non-exclusive, non-transferable, worldwide, limited </span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">license (without the right to sublicense), under Specification Lead&#x27;s applicable intellectual property rights to view, download, use and </span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">reproduce the Specification only for the purpose of internal evaluation. This includes (i) developing applications intended to run on an </span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">implementation of the Specification, provided that such applications do not themselves implement any portion(s) of the Specification, and (ii) </span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">discussing the Specification with any third party; and (iii) excerpting brief portions of the Specification in oral or written communications </span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">which discuss the Specification provided that such excerpts do not in the aggregate constitute a significant portion of the Specification.</span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">2. License for the Distribution of Compliant Implementations. Specification Lead also grants you a perpetual, non-exclusive, non-transferable, </span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">worldwide, fully paid-up, royalty free, limited license (without the right to sublicense) under any applicable copyrights or, subject to the </span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">provisions of subsection 4 below, patent rights it may have covering the Specification to create and/or distribute an Independent </span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">Implementation of the Specification that: (a) fully implements the Specification including all its required interfaces and functionality; (b) does </span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">not modify, subset, superset or otherwise extend the Licensor Name Space, or include any public or protected packages, classes, Java </span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">interfaces, fields or methods within the Licensor Name Space other than those required/authorized by the Specification or Specifications </span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">being implemented; and (c) passes the Technology Compatibility Kit (including satisfying the requirements of the applicable TCK Users </span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">Guide) for such Specification (&quot;Compliant Implementation&quot;). In addition, the foregoing license is expressly conditioned on your not acting </span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">outside its scope. No license is granted hereunder for any other purpose (including, for example, modifying the Specification, other than to the </span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">extent of your fair use rights, or distributing the Specification to third parties). Also, no right, title, or interest in or to any trademarks, service </span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">marks, or trade names of Specification Lead or Specification Lead&#x27;s licensors is granted hereunder. Java, and Java-related logos, marks and </span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">names are trademarks or registered trademarks of Oracle America, Inc. in the U.S. and other countries.</span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">3. Pass-through Conditions. You need not include limitations (a)-(c) from the previous paragraph or any</span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">other particular &quot;pass through&quot; requirements in any license You grant concerning the use of your</span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">Independent Implementation or products derived from it. However, except with respect to</span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">Independent Implementations (and products derived from them) that satisfy limitations (a)-(c) from the</span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">previous paragraph, You may neither: (a) grant or otherwise pass through to your licensees any licenses</span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">under Specification Lead&#x27;s applicable intellectual property rights; nor (b) authorize your licensees to</span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">make any claims concerning their implementation&#x27;s compliance with the Specification in question.</span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">4. Reciprocity Concerning Patent Licenses.</span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">a. With respect to any patent claims covered by the license granted under subparagraph 2</span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">above that would be infringed by all technically feasible implementations of the Specification, such</span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">license is conditioned upon your offering on fair, reasonable and non-discriminatory terms, to any party</span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">seeking it from You, a perpetual, non-exclusive, non-transferable, worldwide license under Your patent</span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">rights which are or would be infringed by all technically feasible implementations of the Specification to</span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">develop, distribute and use a Compliant Implementation.</span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">b With respect to any patent claims owned by Specification Lead and covered by the license</span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">granted under subparagraph 2, whether or not their infringement can be avoided in a technically</span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">feasible manner when implementing the Specification, such license shall terminate with respect to such</span>
<span style="font-family:'Palatino-Roman';font-size:7.91pt;color:#000000">Please</span>
<span style="font-family:'Palatino-Roman';font-size:7.91pt;color:#000000">Recycle</span>

---

## Page 3

<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">claims if You initiate a claim against Specification Lead that it has, in the course of performing its</span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">responsibilities as the Specification Lead, induced any other entity to infringe Your patent rights.</span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">c Also with respect to any patent claims owned by Specification Lead and covered by the license</span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">granted under subparagraph 2 above, where the infringement of such claims can be avoided in a</span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">technically feasible manner when implementing the Specification such license, with respect to such</span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">claims, shall terminate if You initiate a claim against Specification Lead that its making, having made,</span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">using, offering to sell, selling or importing a Compliant Implementation infringes Your patent rights.</span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">5. Definitions. For the purposes of this Agreement: &quot;Independent Implementation&quot; shall mean an</span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">implementation of the Specification that neither derives from any of Specification Lead&#x27;s source code or</span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">binary code materials nor, except with an appropriate and separate license from Specification Lead,</span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">includes any of Specification Lead&#x27;s source code or binary code materials; &quot;Licensor Name Space&quot; shall</span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">mean the public class or interface declarations whose names begin with &quot;java&quot;, &quot;javax&quot;, &quot;com.oracle”,</span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">“com.sun” or their equivalents in any subsequent naming convention adopted by Oracle America, Inc.</span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">through the Java Community Process, or any recognized successors or replacements thereof; and</span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">&quot;Technology Compatibility Kit&quot; or &quot;TCK&quot; shall mean the test suite and accompanying TCK User&#x27;s Guide</span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">provided by Specification Lead which corresponds to the Specification and that was available either (i)</span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">from Specification Lead&#x27;s 120 days before the first release of Your Independent Implementation that</span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">allows its use for commercial purposes, or (ii) more recently than 120 days from such release but against</span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">which You elect to test Your implementation of the Specification.</span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">This Agreement will terminate immediately without notice from Specification Lead if you breach the</span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">Agreement or act outside the scope of the licenses granted above.</span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">DISCLAIMER OF WARRANTIES</span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">THE SPECIFICATION IS PROVIDED &quot;AS IS&quot;. SPECIFICATION LEAD MAKES NO REPRESENTATIONS OR WARRANTIES, EITHER </span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO, WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR </span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">PURPOSE, NON-INFRINGEMENT (INCLUDING AS A CONSEQUENCE OF ANY PRACTICE OR IMPLEMENTATION OF THE </span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">SPECIFICATION), OR THAT THE CONTENTS OF THE SPECIFICATION ARE SUITABLE FOR ANY PURPOSE. This document does not </span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">represent any commitment to release or implement any portion of the Specification in any product. In addition, the Specification could include </span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">technical inaccuracies or typographical errors.</span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">LIMITATION OF LIABILITY</span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">TO THE EXTENT NOT PROHIBITED BY LAW, IN NO EVENT WILL SPECIFICATION LEAD OR ITS LICENSORS BE LIABLE FOR ANY </span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">DAMAGES, INCLUDING WITHOUT LIMITATION, LOST REVENUE, PROFITS OR DATA, OR FOR SPECIAL, INDIRECT, </span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY, </span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">ARISING OUT OF OR RELATED IN ANY WAY TO YOUR HAVING, IMPELEMENTING OR OTHERWISE USING THE SPECIFICATION, </span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">EVEN IF SPECIFICATION LEAD AND/OR ITS LICENSORS HAVE BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.</span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">You will indemnify, hold harmless, and defend Specification Lead and its licensors from any claims arising or resulting from: (i) your use of </span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">the Specification; (ii) the use or distribution of your Java application, applet and/or implementation; and/or (iii) any claims that later versions </span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">or releases of any Specification furnished to you are incompatible with the Specification provided to you under this license.</span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">RESTRICTED RIGHTS LEGEND</span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">U.S. Government: If this Specification is being acquired by or on behalf of the U.S. Government or by a U.S. Government prime contractor or </span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">subcontractor (at any tier), then the Government&#x27;s rights in the Software and accompanying documentation shall be only as set forth in this </span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">license; this is in accordance with 48 C.F.R. 227.7201 through 227.7202-4 (for Department of Defense (DoD) acquisitions) and with 48 C.F.R. </span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">2.101 and 12.212 (for non-DoD acquisitions).</span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">REPORT</span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">If you provide Specification Lead with any comments or suggestions concerning the Specification (&quot;Feedback&quot;), you hereby: (i) agree that such </span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">Feedback is provided on a non-proprietary and non-confidential basis, and (ii) grant Specification Lead a perpetual, non-exclusive, </span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">worldwide, fully paid-up, irrevocable license, with the right to sublicense through multiple levels of sublicensees, to incorporate, disclose, and </span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">use without limitation the Feedback for any purpose.</span>

---

## Page 4

<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">GENERAL TERMS</span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">Any action related to this Agreement will be governed by California law and controlling U.S. federal law. The U.N. Convention for the </span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">International Sale of Goods and the choice of law rules of any jurisdiction will not apply.</span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">The Specification is subject to U.S. export control laws and may be subject to export or import regulations in other countries. Licensee agrees to </span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">comply strictly with all such laws and regulations and acknowledges that it has the responsibility to obtain such licenses to export, re-export </span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">or import as may be required after delivery to Licensee.</span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">This Agreement is the parties&#x27; entire agreement relating to its subject matter. It supersedes all prior or contemporaneous oral or written </span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">communications, proposals, conditions, representations and warranties and prevails over any conflicting or additional terms of any quote, </span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">order, acknowledgment, or other communication between the parties relating to its subject matter during the term of this Agreement. No </span>
<span style="font-family:'Palatino-Roman';font-size:7.43pt;color:#000000">modification to this Agreement will be binding, unless in writing and signed by an authorized representative of each party.</span>

---

## Page 5

<span style="font-family:'Helvetica';font-size:8.99pt;color:#000000">Oracle Corporation</span>
<span style="font-family:'Helvetica';font-size:8.99pt;color:#000000">www.oracle.com</span>

<span style="font-family:'Helvetica';font-size:6.95pt;color:#000000">Submit comments about this document at:  </span><span style="font-family:'Courier-Bold';font-size:8.99pt;font-weight:bold;color:#000000">jsr-221-comments@jcp.org</span>

![](./jdbc4.2-fr-spec_assets/images/p005_img01.png)

---

## Page 6

<span style="font-family:'Palatino-Roman';font-size:19.90pt;color:#000000">Contents</span>

<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">Preface</span>
<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">1</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Typographic Conventions</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">2</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Submitting Feedback</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">2</span>
<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">1.</span>
<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">Introduction</span>
<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">3</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">1.1</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">The JDBC API</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">3</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">1.2</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Platforms</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">3</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">1.3</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Target Audience</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">4</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">1.4</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Acknowledgements</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">4</span>
<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">2.</span>
<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">Goals</span>
<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">7</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">2.1</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">History</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">7</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">2.2</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Overview of Goals</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">7</span>
<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">3.</span>
<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">Summary of New Features</span>
<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">11</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">3.1</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Overview of changes</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">11</span>
<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">4.</span>
<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">Overview</span>
<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">15</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">4.1</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Establishing a Connection</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">15</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">4.2</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Executing SQL Statements and Manipulating Results</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">16</span>

<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">4.2.1</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Support for SQL Advanced Data Types</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">17</span>

<span style="font-family:'Helvetica-Bold';font-size:7.91pt;font-weight:bold;color:#000000">v</span>

---

## Page 7

<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">4.3</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Two-tier Model</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">17</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">4.4</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Three-tier Model</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">18</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">4.5</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">JDBC in the Java EE Platform</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">20</span>
<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">5.</span>
<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">Classes and Interfaces</span>
<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">21</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">5.1</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">The </span><span style="font-family:'Courier';font-size:9.95pt;color:#000000">java.sql</span><span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000"> Package</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">21</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">5.2</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">The </span><span style="font-family:'Courier';font-size:9.95pt;color:#000000">javax.sql</span><span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000"> Package</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">25</span>
<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">6.</span>
<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">Compliance</span>
<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">29</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">6.1</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Definitions</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">29</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">6.2</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Guidelines and Requirements</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">30</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">6.3</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">JDBC 4.2 API Compliance</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">31</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">6.4</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Java EE JDBC Compliance</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">35</span>
<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">7.</span>
<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">Database Metadata</span>
<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">37</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">7.1</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Creating a </span><span style="font-family:'Courier';font-size:9.95pt;color:#000000">DatabaseMetadata</span><span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000"> Object</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">38</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">7.2</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Retrieving General Information</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">38</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">7.3</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Determining Feature Support</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">39</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">7.4</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Data Source Limits</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">39</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">7.5</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">SQL Objects and Their Attributes</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">40</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">7.6</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Transaction Support</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">40</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">7.7</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">New Methods</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">41</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">7.8</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Modified Methods</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">41</span>
<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">8.</span>
<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">Exceptions</span>
<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">43</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">8.1</span>
<span style="font-family:'Courier';font-size:9.95pt;color:#000000">SQLException</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">43</span>

<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">8.1.1</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Support for the Java SE Chained Execeptions</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">44</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">8.1.2</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Navigating SQLExceptions</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">44</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">8.2</span>
<span style="font-family:'Courier';font-size:9.95pt;color:#000000">SQLWarning</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">46</span>

<span style="font-family:'Helvetica-Bold';font-size:7.91pt;font-weight:bold;color:#000000">vi</span>
<span style="font-family:'Helvetica';font-size:7.91pt;color:#000000">JDBC 4.2 Specification • March 2014</span>

<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">8.1.2.1</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Using a For-Each Loop with SQLExceptions</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">45</span>

---

## Page 8

<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">8.3</span>
<span style="font-family:'Courier';font-size:9.95pt;color:#000000">DataTruncation</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">46</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">8.3.1</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Silent Truncation</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">47</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">8.4</span>
<span style="font-family:'Courier';font-size:9.95pt;color:#000000">BatchUpdateException</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">47</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">8.5</span>
<span style="font-family:'Courier';font-size:9.95pt;color:#000000">Categorized SQLExceptions</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">48</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">8.5.1</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">NonTransient SQLExceptions</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">48</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">8.5.2</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Transient SQLExceptions</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">49</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">8.5.3</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">SQLRecoverableException</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">50</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">8.6</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">SQLClientinfoException</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">50</span>
<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">9.</span>
<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">Connections</span>
<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">51</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">9.1</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Types of Drivers</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">52</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">9.2</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">The </span><span style="font-family:'Courier';font-size:9.95pt;color:#000000">Driver</span><span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000"> Interface</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">52</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">9.3</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">The DriverAction Interface</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">53</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">9.4</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">The </span><span style="font-family:'Courier';font-size:9.95pt;color:#000000">DriverManager</span><span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000"> Class</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">54</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">9.5</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">The SQLPermission Class</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">56</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">9.6</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">The </span><span style="font-family:'Courier';font-size:9.95pt;color:#000000">DataSource</span><span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000"> Interface</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">56</span>

<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">9.6.1</span>
<span style="font-family:'Courier';font-size:9.95pt;color:#000000">DataSource</span><span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000"> Properties</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">57</span>

<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">9.6.4</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Closing </span><span style="font-family:'Courier';font-size:9.95pt;color:#000000">Connection</span><span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000"> Objects</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">59</span>

<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">10.</span>
<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">Transactions</span>
<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">61</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">10.1</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Transaction Boundaries and Auto-commit</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">61</span>

<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">10.1.1</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Disabling Auto-commit Mode</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">62</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">10.2</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Transaction Isolation Levels</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">62</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">9.2.1</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Loading a driver that implements java.sql.Driver</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">53</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">9.6.2</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">The JNDI API and Application Portability</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">58</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">9.6.3</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Getting a Connection with a </span><span style="font-family:'Courier';font-size:9.95pt;color:#000000">DataSource</span><span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000"> Object</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">59</span>

<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">9.6.4.1</span>
<span style="font-family:'Courier';font-size:9.95pt;color:#000000">Connection.close</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">60</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">9.6.4.2</span>
<span style="font-family:'Courier';font-size:9.95pt;color:#000000">Connection.isClosed</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">60</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">9.6.4.3</span>
<span style="font-family:'Courier';font-size:9.95pt;color:#000000">Connection.isValid</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">60</span>

<span style="font-family:'Helvetica';font-size:7.91pt;color:#000000">Contents</span>
<span style="font-family:'Helvetica-Bold';font-size:7.91pt;font-weight:bold;color:#000000">vii</span>

---

## Page 9

<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">10.2.1</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Using the </span><span style="font-family:'Courier';font-size:9.95pt;color:#000000">setTransactionIsolation</span><span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000"> Method</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">63</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">10.2.2</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Performance Considerations</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">64</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">10.3</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Savepoints</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">64</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">10.3.1</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Setting and Rolling Back to a Savepoint</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">65</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">10.3.2</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Releasing a Savepoint</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">65</span>
<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">11.</span>
<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">Connection Pooling</span>
<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">67</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">11.1</span>
<span style="font-family:'Courier';font-size:9.95pt;color:#000000">ConnectionPoolDataSource</span><span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000"> and </span><span style="font-family:'Courier';font-size:9.95pt;color:#000000">PooledConnection</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">69</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">11.2</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Connection Events</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">70</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">11.3</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Connection Pooling in a Three-tier Environment</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">71</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">11.4</span>
<span style="font-family:'Courier';font-size:9.95pt;color:#000000">DataSource</span><span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000"> Implementations and Connection Pooling</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">72</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">11.5</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Deployment</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">74</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">11.6</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Reuse of Statements by Pooled Connections</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">75</span>

<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">11.6.1</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Using a Pooled Statement</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">76</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">11.6.2</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Closing a Pooled Statement</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">77</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">11.7</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Statement Events</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">78</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">11.8</span>
<span style="font-family:'Courier';font-size:9.95pt;color:#000000">ConnectionPoolDataSource</span><span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000"> Properties</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">79</span>
<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">12.</span>
<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">Distributed Transactions</span>
<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">81</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">12.1</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Infrastructure</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">81</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">12.2</span>
<span style="font-family:'Courier';font-size:9.95pt;color:#000000">XADataSource</span><span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000"> and </span><span style="font-family:'Courier';font-size:9.95pt;color:#000000">XAConnection</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">84</span>

<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">12.2.1</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Deploying an </span><span style="font-family:'Courier';font-size:9.95pt;color:#000000">XADataSource</span><span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000"> Object</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">85</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">12.2.2</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Getting a Connection</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">86</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">12.3</span>
<span style="font-family:'Courier';font-size:9.95pt;color:#000000">XAResource</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">86</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">12.4</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Transaction Management</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">87</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">12.4.1</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Two-phase Commit</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">88</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">12.5</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Closing the Connection</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">90</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">12.6</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Limitations of the </span><span style="font-family:'Courier';font-size:9.95pt;color:#000000">XAResource</span><span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000"> Interface</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">90</span>
<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">13.</span>
<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">Statements</span>
<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">93</span>

<span style="font-family:'Helvetica-Bold';font-size:7.91pt;font-weight:bold;color:#000000">viii</span>
<span style="font-family:'Helvetica';font-size:7.91pt;color:#000000">JDBC 4.2 Specification • March 2014</span>

---

## Page 10

<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">13.1</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">The </span><span style="font-family:'Courier';font-size:9.95pt;color:#000000">Statement</span><span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000"> Interface</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">93</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">13.1.1</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Creating Statements</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">93</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">13.1.2</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Executing </span><span style="font-family:'Courier';font-size:9.95pt;color:#000000">Statement</span><span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000"> Objects</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">94</span>

<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">13.1.4</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Closing </span><span style="font-family:'Courier';font-size:9.95pt;color:#000000">Statement</span><span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000"> Objects</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">98</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">13.2</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">The </span><span style="font-family:'Courier';font-size:9.95pt;color:#000000">PreparedStatement</span><span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000"> Interface</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">99</span>

<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">13.2.2</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Setting Parameters</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">100</span>

<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">13.3</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">The </span><span style="font-family:'Courier';font-size:9.95pt;color:#000000">CallableStatement</span><span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000"> Interface</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">107</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">13.3.2</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Setting Parameters</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">108</span>

<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">13.1.1.1</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Setting </span><span style="font-family:'Courier';font-size:9.95pt;color:#000000">ResultSet</span><span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000"> Characteristics</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">94</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">13.1.2.1</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Returning a ResultSet object</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">95</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">13.1.2.2</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Returning an Update Count</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">95</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">13.1.2.3</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Returning Unknown or Multiple Results</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">96</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">13.1.3</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Limiting the execution time for </span><span style="font-family:'Courier';font-size:9.95pt;color:#000000">Statement</span><span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000"> Objects</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">98</span>

<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">13.2.1</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Creating a </span><span style="font-family:'Courier';font-size:9.95pt;color:#000000">PreparedStatement</span><span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000"> Object</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">99</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">13.2.1.1</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Setting </span><span style="font-family:'Courier';font-size:9.95pt;color:#000000">ResultSet</span><span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000"> Characteristics</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">99</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">13.2.2.1</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Type Conversions</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">101</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">13.2.2.2</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">National Character Set Conversions</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">102</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">13.2.2.3</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Type Conversions Using the Method </span><span style="font-family:'Courier';font-size:9.95pt;color:#000000">setObject</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">102</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">13.2.2.4</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Setting </span><span style="font-family:'Courier';font-size:9.95pt;color:#000000">NULL</span><span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000"> Parameters</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">103</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">13.2.2.5</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Clearing Parameters</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">104</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">13.2.3</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Describing Outputs and Inputs of a </span><span style="font-family:'Courier';font-size:9.95pt;color:#000000">PreparedStatement</span><span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000"> </span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Object</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">104</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">13.2.4</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Executing a </span><span style="font-family:'Courier';font-size:9.95pt;color:#000000">PreparedStatement</span><span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000"> Object</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">105</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">13.2.4.1</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Returning a </span><span style="font-family:'Courier';font-size:9.95pt;color:#000000">ResultSet</span><span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000"> Object</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">105</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">13.2.4.2</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Returning an Update Count</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">106</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">13.2.4.3</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Returning Unknown or Multiple Results</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">106</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">13.3.1</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Creating a CallableStatement Object</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">107</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">13.3.2.1</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">IN Parameters</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">109</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">13.3.2.2</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">OUT Parameters</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">109</span>

<span style="font-family:'Helvetica';font-size:7.91pt;color:#000000">Contents</span>
<span style="font-family:'Helvetica-Bold';font-size:7.91pt;font-weight:bold;color:#000000">ix</span>

---

## Page 11

<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">13.3.2.3</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">INOUT Parameters</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">110</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">13.3.2.4</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Clearing Parameters</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">110</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">13.3.3</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Executing a </span><span style="font-family:'Courier';font-size:9.95pt;color:#000000">CallableStatement</span><span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000"> Object</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">110</span>

<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">13.3.3.1</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Returning a Single </span><span style="font-family:'Courier';font-size:9.95pt;color:#000000">ResultSet</span><span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000"> Object</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">110</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">13.3.3.2</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Returning an Update Count</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">111</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">13.3.3.3</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Returning Unknown or Multiple Results</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">111</span>

<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">13.3.3.4</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">REF Cursor Support</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">112</span>

<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">13.4</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Escape Syntax</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">113</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">13.4.1</span>
<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">Scalar Functions</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">114</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">13.4.2</span>
<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">Date and Time Literals</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">114</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">13.4.3</span>
<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">Outer Joins</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">115</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">13.4.4</span>
<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">Stored Procedures and Functions</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">116</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">13.4.5</span>
<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">LIKE Escape Characters</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">116</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">13.4.6</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Limiting Returned Rows Escape</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">117</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">13.5</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Performance Hints</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">117</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">13.6</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Retrieving Auto Generated Values</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">118</span>
<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">14.</span>
<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">Batch Updates</span>
<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">121</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">14.1</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Description of Batch Updates</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">121</span>

<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">14.1.1</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Statements</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">121</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">14.1.2</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Successful Execution</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">122</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">14.1.3</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Handling Failures during Execution</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">123</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">14.1.4</span>
<span style="font-family:'Courier';font-size:9.95pt;color:#000000">PreparedStatement</span><span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000"> Objects</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">124</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">14.1.5</span>
<span style="font-family:'Courier';font-size:9.95pt;color:#000000">CallableStatement</span><span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000"> Objects</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">126</span>

<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">15.</span>
<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">Result Sets</span>
<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">127</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">15.1</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Kinds of </span><span style="font-family:'Courier';font-size:9.95pt;color:#000000">ResultSet</span><span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000"> Objects</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">127</span>

<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">15.1.1</span>
<span style="font-family:'Courier';font-size:9.95pt;color:#000000">ResultSet</span><span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000"> Types</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">127</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">15.1.2</span>
<span style="font-family:'Courier';font-size:9.95pt;color:#000000">ResultSet</span><span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000"> Concurrency</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">128</span>

<span style="font-family:'Helvetica-Bold';font-size:7.91pt;font-weight:bold;color:#000000">x</span>
<span style="font-family:'Helvetica';font-size:7.91pt;color:#000000">JDBC 4.2 Specification • March 2014</span>

---

## Page 12

<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">15.1.3</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">ResultSet Holdability</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">129</span>

<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">15.1.3.1</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Determining ResultSet Holdability</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">129</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">15.1.4</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Specifying </span><span style="font-family:'Courier';font-size:9.95pt;color:#000000">ResultSet</span><span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000"> Type, Concurrency and Holdability</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">130</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">15.2</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Creating and Manipulating </span><span style="font-family:'Courier';font-size:9.95pt;color:#000000">ResultSet</span><span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000"> Objects</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">130</span>

<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">15.2.1</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Creating </span><span style="font-family:'Courier';font-size:9.95pt;color:#000000">ResultSet</span><span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000"> Objects</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">130</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">15.2.2</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Cursor Movement</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">131</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">15.2.3</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Retrieving Values</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">132</span>

<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">15.2.3.1</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Data Type Conversions</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">133</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">15.2.3.2</span>
<span style="font-family:'Courier';font-size:9.95pt;color:#000000">ResultSet</span><span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000"> Metadata</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">133</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">15.2.3.3</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Retrieving </span><span style="font-family:'Courier';font-size:9.95pt;color:#000000">NULL</span><span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000"> values</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">133</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">15.2.4.1</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Updating a Row</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">134</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">15.2.4.2</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Deleting a Row</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">135</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">15.2.4.3</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Inserting a Row</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">137</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">15.2.4.4</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Positioned Updates and Deletes</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">138</span>

<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">15.2.4</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Modifying </span><span style="font-family:'Courier';font-size:9.95pt;color:#000000">ResultSet</span><span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000"> Objects</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">134</span>

<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">15.2.5</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Closing a </span><span style="font-family:'Courier';font-size:9.95pt;color:#000000">ResultSet</span><span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000"> Object</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">139</span>

<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">16.</span>
<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">Advanced Data Types</span>
<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">141</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">16.1</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Taxonomy of SQL Types</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">141</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">16.2</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Mapping of Advanced Data Types</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">143</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">16.3</span>
<span style="font-family:'Courier';font-size:9.95pt;color:#000000">Blob</span><span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">,</span><span style="font-family:'Courier';font-size:9.95pt;color:#000000"> Clob </span><span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">and </span><span style="font-family:'Courier';font-size:9.95pt;color:#000000">NClob</span><span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000"> Objects</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">143</span>

<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">16.3.1</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Blob, </span><span style="font-family:'Courier';font-size:9.95pt;color:#000000">Clob </span><span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">and </span><span style="font-family:'Courier';font-size:9.95pt;color:#000000">NClob </span><span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Implementations</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">143</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">16.3.2</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Creating Blob, </span><span style="font-family:'Courier';font-size:9.95pt;color:#000000">Clob </span><span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">and </span><span style="font-family:'Courier';font-size:9.95pt;color:#000000">NClob</span><span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000"> Objects</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">144</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">16.3.3</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Retrieving </span><span style="font-family:'Courier';font-size:9.95pt;color:#000000">BLOB</span><span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">, </span><span style="font-family:'Courier';font-size:9.95pt;color:#000000">Clob </span><span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">and </span><span style="font-family:'Courier';font-size:9.95pt;color:#000000">NClob</span><span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000"> Values in a ResultSet</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">144</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">16.3.4</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Accessing </span><span style="font-family:'Courier';font-size:9.95pt;color:#000000">Blob</span><span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">, </span><span style="font-family:'Courier';font-size:9.95pt;color:#000000">Clob </span><span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">and </span><span style="font-family:'Courier';font-size:9.95pt;color:#000000">NClob</span><span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000"> Object Data</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">145</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">16.3.5</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Storing </span><span style="font-family:'Courier';font-size:9.95pt;color:#000000">Blob</span><span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">, </span><span style="font-family:'Courier';font-size:9.95pt;color:#000000">Clob </span><span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">and </span><span style="font-family:'Courier';font-size:9.95pt;color:#000000">NClob</span><span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000"> Objects</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">145</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">16.3.6</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Altering </span><span style="font-family:'Courier';font-size:9.95pt;color:#000000">Blob</span><span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">, </span><span style="font-family:'Courier';font-size:9.95pt;color:#000000">Clob </span><span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">and </span><span style="font-family:'Courier';font-size:9.95pt;color:#000000">NClob</span><span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000"> Objects</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">146</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">16.3.7</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Releasing </span><span style="font-family:'Courier';font-size:9.95pt;color:#000000">Blob</span><span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">, </span><span style="font-family:'Courier';font-size:9.95pt;color:#000000">Clob </span><span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">and </span><span style="font-family:'Courier';font-size:9.95pt;color:#000000">NClob</span><span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000"> Resources</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">147</span>

<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">16.4</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">SQLXML Objects</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">147</span>

<span style="font-family:'Helvetica';font-size:7.91pt;color:#000000">Contents</span>
<span style="font-family:'Helvetica-Bold';font-size:7.91pt;font-weight:bold;color:#000000">xi</span>

---

## Page 13

<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">16.4.1</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Creating SQLXML Objects</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">147</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">16.4.2</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Retrieving </span><span style="font-family:'Courier';font-size:9.95pt;color:#000000">SQLXML</span><span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000"> values in a ResultSet</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">148</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">16.4.3</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Accessing </span><span style="font-family:'Courier';font-size:9.95pt;color:#000000">SQLXML</span><span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000"> Object Data</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">148</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">16.4.4</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Storing </span><span style="font-family:'Courier';font-size:9.95pt;color:#000000">SQLXML</span><span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000"> Objects</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">149</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">16.4.5</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Initializing </span><span style="font-family:'Courier';font-size:9.95pt;color:#000000">SQLXML</span><span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000"> Objects</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">150</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">16.4.6</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Releasing </span><span style="font-family:'Courier';font-size:9.95pt;color:#000000">SQLXML</span><span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000"> Resources</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">151</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">16.5</span>
<span style="font-family:'Courier';font-size:9.95pt;color:#000000">Array</span><span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000"> Objects</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">151</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">16.5.1</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Array</span><span style="font-family:'Courier';font-size:9.95pt;color:#000000"> </span><span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Implementations</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">151</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">16.5.2</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Creating Array Objects</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">152</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">16.5.3</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Retrieving </span><span style="font-family:'Courier';font-size:9.95pt;color:#000000">Array</span><span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000"> Objects</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">152</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">16.5.4</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Storing </span><span style="font-family:'Courier';font-size:9.95pt;color:#000000">Array</span><span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000"> Objects</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">153</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">16.5.5</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Updating </span><span style="font-family:'Courier';font-size:9.95pt;color:#000000">Array</span><span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000"> Objects</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">153</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">16.5.6</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Releasing </span><span style="font-family:'Courier';font-size:9.95pt;color:#000000">Array</span><span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000"> Resources</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">154</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">16.6</span>
<span style="font-family:'Courier';font-size:9.95pt;color:#000000">Ref</span><span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000"> Objects</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">154</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">16.6.1</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Retrieving </span><span style="font-family:'Courier';font-size:9.95pt;color:#000000">REF</span><span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000"> Values</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">154</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">16.6.2</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Retrieving the Referenced Value</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">155</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">16.6.3</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Storing </span><span style="font-family:'Courier';font-size:9.95pt;color:#000000">Ref</span><span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000"> Objects</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">155</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">16.6.4</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Storing the Referenced Value</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">155</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">16.6.5</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Metadata</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">156</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">16.7</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Distinct Types</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">156</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">16.7.1</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Retrieving Distinct Types</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">156</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">16.7.2</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Storing Distinct Types</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">157</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">16.7.3</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Metadata</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">157</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">16.8</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Structured Types</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">158</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">16.8.1</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Creating Structured Objects</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">158</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">16.8.2</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Retrieving Structured Types</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">159</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">16.8.3</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Storing Structured Types</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">159</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">16.8.4</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Metadata</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">159</span>

<span style="font-family:'Helvetica-Bold';font-size:7.91pt;font-weight:bold;color:#000000">xii</span>
<span style="font-family:'Helvetica';font-size:7.91pt;color:#000000">JDBC 4.2 Specification • March 2014</span>

---

## Page 14

<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">16.9</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Datalinks</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">160</span>

<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">16.9.1</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Retrieving References to External Data</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">160</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">16.9.2</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Storing References to External Data</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">160</span>

<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">16.9.3</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Metadata</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">161</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">16.10 RowId Objects</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">161</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">16.10.1 Lifetime of RowId Validity</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">161</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">16.10.2 Retrieving RowId Values</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">162</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">16.10.3 Using RowId Values</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">162</span>
<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">17.</span>
<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">Customized Type Mapping</span>
<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">163</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">17.1</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">The Type Mapping</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">163</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">17.2</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Class Conventions</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">164</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">17.3</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Streams of SQL Data</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">165</span>

<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">17.3.1</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Retrieving Data</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">165</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">17.3.2</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Storing Data</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">166</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">17.4</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Examples</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">167</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">17.4.1</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">An SQL Structured Type</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">167</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">17.4.2</span>
<span style="font-family:'Courier';font-size:9.95pt;color:#000000">SQLData</span><span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000"> Implementations</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">169</span>

<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">17.4.3</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Mirroring SQL Inheritance in the Java Programming Language</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">173</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">17.4.4</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Example Mapping of SQL </span><span style="font-family:'Courier';font-size:9.95pt;color:#000000">DISTINCT</span><span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000"> Type</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">174</span>

<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">17.5</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Effect of Transform Groups</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">175</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">17.6</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Generality of the Approach</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">176</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">17.7</span>
<span style="font-family:'Courier';font-size:9.95pt;color:#000000">NULL</span><span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000"> Data</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">176</span>
<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">18.</span>
<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">Relationship to Connectors</span>
<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">179</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">18.1</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">System Contracts</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">179</span>

<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">18.2</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Mapping Connector System Contracts to JDBC Interfaces</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">180</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">18.3</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Packaging JDBC Drivers in Connector RAR File Format</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">181</span>

<span style="font-family:'Helvetica';font-size:7.91pt;color:#000000">Contents</span>
<span style="font-family:'Helvetica-Bold';font-size:7.91pt;font-weight:bold;color:#000000">xiii</span>

---

## Page 15

<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">19.</span>
<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">Wrapper Interface</span>
<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">185</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">19.1</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Wrapper interface methods</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">185</span>

<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">19.1.1</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">unwrap method</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">186</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">19.1.2</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">isWrapperFor method</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">186</span>

<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">A. Revision History</span>
<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">187</span>
<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">B. Data Type Conversion Tables</span>
<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">189</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">B.1</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">JDBC Types Mapped to Java Types</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">189</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">B.2</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Java Types Mapped to JDBC Types</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">191</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">B.3</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">JDBC Types Mapped to Java Object Types</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">192</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">B.4</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Java Object Types Mapped to JDBC Types</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">194</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">B.5</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Conversions by setObject and setNull from Java Object Types to JDBC </span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Types</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">196</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">B.6</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">Type Conversions Supported by ResultSet getter Methods</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">198</span>
<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">C. Scalar Functions</span>
<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">203</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">C.1</span>
<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">NUMERIC FUNCTIONS</span><span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000"> </span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">203</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">C.2</span>
<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">STRING FUNCTIONS</span><span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000"> </span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">204</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">C.3</span>
<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">TIME and DATE FUNCTIONS</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">205</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">C.4</span>
<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">SYSTEM FUNCTIONS</span><span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000"> </span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">206</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">C.5</span>
<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">CONVERSION FUNCTIONS</span>
<span style="font-family:'Palatino-Roman';font-size:9.95pt;color:#000000">206</span>
<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">D. Related Documents</span>
<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">209</span>

<span style="font-family:'Helvetica-Bold';font-size:7.91pt;font-weight:bold;color:#000000">xiv</span>
<span style="font-family:'Helvetica';font-size:7.91pt;color:#000000">JDBC 4.2 Specification • March 2014</span>

---

## Page 16

<span style="font-family:'Palatino-Roman';font-size:20.98pt;color:#000000">Preface</span>

<span style="font-family:'ZapfDingbats';font-size:5.99pt;color:#000000">I</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> “JDBC: A Java SQL API”</span>
<span style="font-family:'ZapfDingbats';font-size:5.99pt;color:#000000">I</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> “JDBC 2.1 API”</span>
<span style="font-family:'ZapfDingbats';font-size:5.99pt;color:#000000">I</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> “JDBC 2.0 Standard Extension API”</span>
<span style="font-family:'ZapfDingbats';font-size:5.99pt;color:#000000">I</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> “JDBC 3.0 Specification”</span>

<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">https://jcp.org/en/jsr/detail?id=221            </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">This document supersedes and consolidates the content of these predecessor </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">specifications:</span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">This document introduces a range of new features for the JDBC API and is combined </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">with various specification improvements that focus on features introduced in or </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">before the JDBC 3.0 API. Where possible, any adjustment to the JDBC 3.0 API is </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">marked for easy identification - look for the JDBC 4.2 API demarcation for specific </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">features introduced in this revised and updated specification.</span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">Readers can also download the API specification (Javadoc</span><span style="font-family:'Palatino-Roman';font-size:7.55pt;color:#000000">TM</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> API and comments) for </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">a complete and precise definition of JDBC classes and interfaces. This documentation </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">is available from the download page at</span>

<span style="font-family:'Helvetica-Bold';font-size:7.91pt;font-weight:bold;color:#000000">1</span>

---

## Page 17

<span style="font-family:'Palatino-Roman';font-size:19.90pt;color:#000000">Typographic Conventions</span>
<span style="font-family:'Helvetica-Bold';font-size:6.95pt;font-weight:bold;color:#000000">Typeface</span>
<span style="font-family:'Helvetica-Bold';font-size:6.95pt;font-weight:bold;color:#000000">Meaning</span>
<span style="font-family:'Helvetica-Bold';font-size:6.95pt;font-weight:bold;color:#000000">Examples</span>
<span style="font-family:'Courier';font-size:8.39pt;color:#000000">AaBbCc123</span>
<span style="font-family:'Palatino-Roman';font-size:8.39pt;color:#000000">The names of commands, files, </span>
<span style="font-family:'Palatino-Roman';font-size:8.39pt;color:#000000">and directories; on-screen </span>
<span style="font-family:'Palatino-Roman';font-size:8.39pt;color:#000000">computer output</span>
<span style="font-family:'Courier-Bold';font-size:8.99pt;font-weight:bold;color:#000000">AaBbCc123</span>
<span style="font-family:'Palatino-Roman';font-size:8.39pt;color:#000000">What you type, when </span>
<span style="font-family:'Palatino-Roman';font-size:8.39pt;color:#000000">contrasted with on-screen </span>
<span style="font-family:'Palatino-Roman';font-size:8.39pt;color:#000000">computer output</span>
<span style="font-family:'Palatino-Italic';font-size:8.39pt;font-style:italic;color:#000000">AaBbCc123</span>
<span style="font-family:'Palatino-Roman';font-size:8.39pt;color:#000000">Book titles, new words or </span>
<span style="font-family:'Palatino-Roman';font-size:8.39pt;color:#000000">terms, words to be emphasized</span>

<span style="font-family:'Palatino-Roman';font-size:8.39pt;color:#000000">Command-line variable; </span>
<span style="font-family:'Palatino-Roman';font-size:8.39pt;color:#000000">replace with a real name or </span>
<span style="font-family:'Palatino-Roman';font-size:8.39pt;color:#000000">value</span>

<span style="font-family:'Palatino-Roman';font-size:19.90pt;color:#000000">Submitting Feedback</span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">Please send any comments and questions concerning this specification to:</span>
<span style="font-family:'Courier-Bold';font-size:8.99pt;font-weight:bold;color:#000000">    jsr-221-comments@jcp.org</span>

<span style="font-family:'Helvetica-Bold';font-size:7.91pt;font-weight:bold;color:#000000">2</span>
<span style="font-family:'Helvetica';font-size:7.91pt;color:#000000">JDBC 4.2 Specification • March 2014</span>

<span style="font-family:'Palatino-Roman';font-size:8.39pt;color:#000000">Edit your </span><span style="font-family:'Courier';font-size:8.39pt;color:#000000">.login</span><span style="font-family:'Palatino-Roman';font-size:8.39pt;color:#000000"> file.</span>
<span style="font-family:'Palatino-Roman';font-size:8.39pt;color:#000000">Use </span><span style="font-family:'Courier';font-size:8.39pt;color:#000000">ls -a</span><span style="font-family:'Palatino-Roman';font-size:8.39pt;color:#000000"> to list all files.</span>
<span style="font-family:'Courier';font-size:8.39pt;color:#000000">% You have mail</span><span style="font-family:'Palatino-Roman';font-size:8.39pt;color:#000000">.</span>
<span style="font-family:'Courier';font-size:8.39pt;color:#000000">% </span><span style="font-family:'Courier-Bold';font-size:8.99pt;font-weight:bold;color:#000000">su</span>
<span style="font-family:'Courier';font-size:8.39pt;color:#000000">Password:</span>
<span style="font-family:'Palatino-Roman';font-size:8.39pt;color:#000000">Read Chapter 6 in the </span><span style="font-family:'Palatino-Italic';font-size:8.39pt;font-style:italic;color:#000000">User’s Guide</span><span style="font-family:'Palatino-Roman';font-size:8.39pt;color:#000000">.</span>
<span style="font-family:'Palatino-Roman';font-size:8.39pt;color:#000000">These are called </span><span style="font-family:'Palatino-Italic';font-size:8.39pt;font-style:italic;color:#000000">class</span><span style="font-family:'Palatino-Roman';font-size:8.39pt;color:#000000"> options.</span>
<span style="font-family:'Palatino-Roman';font-size:8.39pt;color:#000000">You </span><span style="font-family:'Palatino-Italic';font-size:8.39pt;font-style:italic;color:#000000">must</span><span style="font-family:'Palatino-Roman';font-size:8.39pt;color:#000000"> be superuser to do this.</span>
<span style="font-family:'Palatino-Roman';font-size:8.39pt;color:#000000">To delete a file, type </span><span style="font-family:'Courier';font-size:8.39pt;color:#000000">rm</span><span style="font-family:'Palatino-Roman';font-size:8.39pt;color:#000000"> </span><span style="font-family:'Palatino-Italic';font-size:8.39pt;font-style:italic;color:#000000">filename</span><span style="font-family:'Palatino-Roman';font-size:8.39pt;color:#000000">.</span>

---

## Page 18

<span style="font-family:'Helvetica';font-size:8.39pt;color:#000000">CHAPTER</span><span style="font-family:'Helvetica-Bold';font-size:17.98pt;font-weight:bold;color:#000000"> 1</span>

<span style="font-family:'Palatino-Roman';font-size:20.98pt;color:#000000">Introduction</span>

<span style="font-family:'Palatino-Roman';font-size:19.90pt;color:#000000">1.1</span>
<span style="font-family:'Palatino-Roman';font-size:19.90pt;color:#000000">The JDBC API</span>

<span style="font-family:'Palatino-Roman';font-size:19.90pt;color:#000000">1.2</span>
<span style="font-family:'Palatino-Roman';font-size:19.90pt;color:#000000">Platforms</span>

<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">The JDBC</span><span style="font-family:'Palatino-Roman';font-size:6.35pt;color:#000000">TM </span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">API provides programmatic access to relational data from the Java</span><span style="font-family:'Palatino-Roman';font-size:6.35pt;color:#000000">TM</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">programming language. Using the JDBC API, applications written in the Java </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">programming language can execute SQL statements, retrieve results, and propagate </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">changes back to an underlying data source. The JDBC API can also be used to </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">interact with multiple data sources in a distributed, heterogeneous environment. </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">The JDBC API is based on the X/Open SQL CLI, which is also the basis for ODBC. </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">JDBC provides a natural and easy-to-use mapping from the Java programming </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">language to the abstractions and concepts defined in the X/Open CLI and SQL </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">standards.</span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">Since its introduction in January 1997, the JDBC API has become widely accepted </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">and implemented. The flexibility of the API allows for a broad range of </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">implementations.</span>

<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">The JDBC API is part of the Java platform, which includes the Java</span><span style="font-family:'Palatino-Roman';font-size:6.35pt;color:#000000">TM</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> Standard </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">Edition (Java</span><span style="font-family:'Palatino-Roman';font-size:6.35pt;color:#000000">TM </span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">SE</span><span style="font-family:'Palatino-Roman';font-size:6.35pt;color:#000000"> </span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">) and the Java</span><span style="font-family:'Palatino-Roman';font-size:6.35pt;color:#000000">TM</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> Enterprise Edition (Java</span><span style="font-family:'Palatino-Roman';font-size:6.35pt;color:#000000">TM </span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">EE). The JDBC API is </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">divided into two packages: </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">java.sql</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> and </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">javax.sql</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">. Both packages are included </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">in the Java SE and Java EE platforms.</span>

<span style="font-family:'Helvetica-Bold';font-size:7.91pt;font-weight:bold;color:#000000">3</span>

---

## Page 19

<span style="font-family:'Palatino-Roman';font-size:19.90pt;color:#000000">1.3</span>
<span style="font-family:'Palatino-Roman';font-size:19.90pt;color:#000000">Target Audience</span>

<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">This specification is targeted primarily towards the vendors of these types of </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">products:</span>
<span style="font-family:'ZapfDingbats';font-size:5.99pt;color:#000000">I</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> drivers that implement the JDBC API</span>
<span style="font-family:'ZapfDingbats';font-size:5.99pt;color:#000000">I</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> application servers providing middle-tier services above the driver layer</span>
<span style="font-family:'ZapfDingbats';font-size:5.99pt;color:#000000">I</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> tools that use the JDBC API to provide services such as application generation </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">This specification is also intended to serve the following purposes:</span>
<span style="font-family:'ZapfDingbats';font-size:5.99pt;color:#000000">I</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> an introduction for end-users whose applications use the JDBC API</span>
<span style="font-family:'ZapfDingbats';font-size:5.99pt;color:#000000">I</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> a starting point for developers of other APIs layered on top of the JDBC API</span>

<span style="font-family:'Palatino-Roman';font-size:19.90pt;color:#000000">1.4</span>
<span style="font-family:'Palatino-Roman';font-size:19.90pt;color:#000000">Acknowledgements</span>

<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">The JDBC 4.2 specification work is being conducted as part of JSR-221 under the Java </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">Community Process. This specification is the result of the collaborative efforts of the </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">JDBC 4.2 Expert Group whose individual members contributed countess hours to </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">ensure the success of this specification.  We would like to thank the following </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">members for their contributions:</span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">Lance Andersen, Oracle (Specification Lead)</span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">Mark Biamonte, DataDirect Technologies</span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">Volker Berlin</span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">Jesse Davis, DataDirect Technologies</span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">Christopher Farrar, IBM</span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">John Goodson, DataDirect Technologies</span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">Karim Khamis, Sybase</span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">Mark Matthews, Oracle</span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">Marco Paskamp, SAP AG</span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">Ajit Sabnis, Sybase</span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">Douglas Surber, Oracle</span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">Joe Weinstein, Oracle</span>

<span style="font-family:'Helvetica-Bold';font-size:7.91pt;font-weight:bold;color:#000000">4</span>
<span style="font-family:'Helvetica';font-size:7.91pt;color:#000000">JDBC 4.2 Specification • March 2014</span>

---

## Page 20

<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">Thanks also go to the many people behind the scenes who have helped and </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">supported this effort: Ian Evans , Jeff Dinkins, Rick Hillegas, Eric Jendrock, Knut </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">Anders Hatlen, and Dag Wanvik.</span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">Last, but not least, we would like to thank the previous JDBC specification leads for </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">their contributions to the success of JDBC:   Graham Hamilton, Rick Cattell, Seth </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">White, Jon Ellis, Linda Ho and Jonathan Bruce. </span>

<span style="font-family:'Helvetica';font-size:7.91pt;color:#000000">Chapter 1</span>
<span style="font-family:'Helvetica';font-size:7.91pt;color:#000000">Introduction</span>
<span style="font-family:'Helvetica-Bold';font-size:7.91pt;font-weight:bold;color:#000000">5</span>

---

## Page 21

<span style="font-family:'Helvetica-Bold';font-size:7.91pt;font-weight:bold;color:#000000">6</span>
<span style="font-family:'Helvetica';font-size:7.91pt;color:#000000">JDBC 4.2 Specification • March 2014</span>

---

## Page 22

<span style="font-family:'Helvetica';font-size:8.39pt;color:#000000">CHAPTER</span><span style="font-family:'Helvetica-Bold';font-size:17.98pt;font-weight:bold;color:#000000"> 2</span>

<span style="font-family:'Palatino-Roman';font-size:20.98pt;color:#000000">Goals</span>

<span style="font-family:'Palatino-Roman';font-size:19.90pt;color:#000000">2.1</span>
<span style="font-family:'Palatino-Roman';font-size:19.90pt;color:#000000">History</span>

<span style="font-family:'Palatino-Roman';font-size:19.90pt;color:#000000">2.2</span>
<span style="font-family:'Palatino-Roman';font-size:19.90pt;color:#000000">Overview of Goals</span>

<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">1. Fit into the Java EE and Java SE platforms </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">The JDBC API is a mature technology, having first been specified in January 1997. In </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">its initial release, the JDBC API focused on providing a basic call-level interface to </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">SQL databases. The JDBC 2.1 specification and the 2.0 Optional Package </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">specification then broadened the scope of the API to include support for more </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">advanced applications and for the features required by application servers to </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">manage use of the JDBC API on behalf of their applications.</span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">The JDBC 3.0 specification operated with the stated goal to round out the API by </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">filling in smaller areas of missing functionality. With JDBC 4.2, our goals are two </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">fold: Improve the Ease-of-Development experience for all developers working with </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">SQL in the Java platform. Secondly, provide a range of enterprise level features to </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">expose JDBC to a richer set of tools and APIs to manage JDBC resources.</span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">The following list outlines the goals and design philosophy for the JDBC API in </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">general and the JDBC 4.2 API in particular: </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">The JDBC API is a constituent technology of the Java platform. The JDBC 4.2 </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">API should be aligned with the overall direction of the Java Enterprise Edition </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">and Java Standard Edition platforms. In addition, recent developments with </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">the Java SE platform have exposed a range of new features and language </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">improvements that are extensively used in this specification.</span>

<span style="font-family:'Helvetica-Bold';font-size:7.91pt;font-weight:bold;color:#000000">7</span>

---

## Page 23

<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">2. Be consistent with SQL:2003</span>

<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">The JDBC API provides programmatic access from applications written in the </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">Java programming language to standard SQL. JDBC 3.0 sought to ensure it’s </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">support for a subset of the SQL99 features that were likely to be widely </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">supported by the industry. Similarly for JDBC 4.2, support for SQL:2003 is </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">focused on the major components of this specification that we anticipate will </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">be supported for the foreseeable future.</span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">The JDBC API strives to provide high-bandwidth access to features commonly </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">supported across different vendor implementations. The goal is to provide a </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">degree of feature access comparable to what can be achieved by native </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">applications. However, the API must be general and flexible enough to allow </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">for a wide range of implementations. </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">The focus of the JDBC API has always been on accessing relational data from </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">the Java programming language. This goal, previously stated in the JDBC 3.0 </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">API remains core to the principles on which this specification is built. The </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">provision of ease of development themed improvements including APIs and </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">utilities continue to focus on the needs for the SQL based software </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">development from the Java platform. Similarly to previous specifications, this </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">does not preclude interacting with additional technologies such as XML, </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">CORBA and non-relational data.</span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">The JDBC API presents a standard API to access a wide range of underlying </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">data sources or legacy systems. Implementation differences are made </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">transparent through JDBC API abstractions, making it a valuable target </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">platform for tools vendors who want to create portable tools and applications.</span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">Because it is a “call-level” interface from the Java programming language to </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">SQL, the JDBC API is also suitable as a base layer for higher-level facilities </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">such as Enterprise JavaBeans (EJB) container-managed persistence, SQLJ and </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">the JDBC RowSet implementation. </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">The JDBC API is intended to be a simple-to-use, straight forward interface </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">upon which more complex entities can be built. This goal is achieved by </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">defining many compact, single-purpose methods instead of a smaller number </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">of complex, multipurpose ones with control flag parameters. </span>

<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">3. Offer vendor-neutral access to common features </span>

<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">4. Maintain the focus on SQL </span>

<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">5. Provide a foundation for tools and higher-level APIs</span>

<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">6. Keep it simple </span>

<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">7. Enhance reliability, availability, and scalability</span>

<span style="font-family:'Helvetica-Bold';font-size:7.91pt;font-weight:bold;color:#000000">8</span>
<span style="font-family:'Helvetica';font-size:7.91pt;color:#000000">JDBC 4.2 Specification • March 2014</span>

---

## Page 24

<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">9. Close Association with JDBC RowSet implementations</span>

<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">10. Allow forward compatibility with Connectors </span>

<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">11. Specify requirements unambiguously</span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">Reliability, availability, and scalability are the themes of the Java EE and Java </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">SE platforms, as well as the direction for future Java platforms. The JDBC API </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">stays true to these themes by enhancing support in several areas, including </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">resource management, the reuse of prepared statements across logical </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">connections, and error handling. </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">8. Maintain backward compatibility with existing applications and drivers</span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">Existing JDBC technology-enabled drivers ( JDBC drivers) and the applications </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">that use them must continue to work in an implementation of the Java virtual </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">machine that supports the JDBC 4.2 API. Applications that use only features </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">defined in earlier releases of the JDBC API will not require changes to continue </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">running. It should be straightforward for existing applications to migrate to </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">JDBC 4.2 technology. </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">Java SE contains a standard JDBC RowSet implementation as specified in JDBC </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">RowSet Implementations (JSR-114). This specification will provide a set of </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">utilities described at both the utility class level and the Meta Data language </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">level. This will allow developers to easily migrate JDBC-technology enabled </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">applications towards the JDBC RowSet model that enables disconnected data </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">source access in addition to the ability to manage relational data stores from an </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">XML stand-point.</span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">The Connector architecture defines a standard way to package and deploy a </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">resource adapter that allows a Java EE container to integrate its connection, </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">transaction, and security management with those of an external resource. The </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">JDBC API provides the migration path for JDBC drivers to the Connector </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">architecture. It should be possible for vendors whose products use JDBC </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">technology to move incrementally towards implementing the Connector API. </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">The expectation is that JDBC driver vendors will write resource manager </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">wrappers around their existing data source implementations so that they can </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">be reused in a Connector framework. </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">The requirements for JDBC compliance need to be unambiguous and easy to </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">identify. The JDBC specification and the API documentation (Javadoc) will </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">clarify which features are required and which are optional.</span>

<span style="font-family:'Helvetica';font-size:7.91pt;color:#000000">Chapter 2</span>
<span style="font-family:'Helvetica';font-size:7.91pt;color:#000000">Goals</span>
<span style="font-family:'Helvetica-Bold';font-size:7.91pt;font-weight:bold;color:#000000">9</span>

---

## Page 25

<span style="font-family:'Helvetica-Bold';font-size:7.91pt;font-weight:bold;color:#000000">10</span>
<span style="font-family:'Helvetica';font-size:7.91pt;color:#000000">JDBC 4.2 Specification • March 2014</span>

---

## Page 26

<span style="font-family:'Helvetica';font-size:8.39pt;color:#000000">CHAPTER</span><span style="font-family:'Helvetica-Bold';font-size:17.98pt;font-weight:bold;color:#000000"> 3</span>

<span style="font-family:'Palatino-Roman';font-size:20.98pt;color:#000000">Summary of New Features</span>

<span style="font-family:'Palatino-Roman';font-size:19.90pt;color:#000000">3.1</span>
<span style="font-family:'Palatino-Roman';font-size:19.90pt;color:#000000">Overview of changes</span>

<span style="font-family:'ZapfDingbats';font-size:5.99pt;color:#000000">I</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> Added support for </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">REF CURSOR</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">.</span>
<span style="font-family:'ZapfDingbats';font-size:5.99pt;color:#000000">I</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> Support for large update counts</span>

<span style="font-family:'ZapfDingbats';font-size:5.99pt;color:#000000">I</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> Addition of the java.sql.DriverAction interface</span>
<span style="font-family:'ZapfDingbats';font-size:5.99pt;color:#000000">I</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> Addition of the java.sql.SQLType interface</span>
<span style="font-family:'ZapfDingbats';font-size:5.99pt;color:#000000">I</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> Addition of the java.sql.JDBCType Enum</span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">The JDBC 4.2 API introduces new material and changes in the following areas:</span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">The REF CURSOR data type is supported by several databases to return a </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">result set from a stored procedure.</span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">JDBC methods that return an update count currently return an int value. </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">This has caused problems as DataSets continue to grow, in certain </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">environments.</span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">This interface may be implemented by a driver that wants to be notified by </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">DriverManager when the driver is deregistered.</span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">An interface used to create an object that represents a generic SQL Type, </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">called a JDBC type or a vendor specific type.</span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">An Enum used to identify generic SQL Types, called JDBCType. The intent is to </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">use JDBCType in place of the constants, defined in Types.java.</span>
<span style="font-family:'ZapfDingbats';font-size:5.99pt;color:#000000">I</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> Additional Mappings to Table B-4, Mapping from Java Object to JDBC Types</span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">Added support to map java.time.LocalDate to JDBC DATE.</span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">Added support to map java.time.LocalTime to JDBC TIME</span>

<span style="font-family:'Helvetica-Bold';font-size:7.91pt;font-weight:bold;color:#000000">11</span>

---

## Page 27

<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">Added support to map java.time.LocalDateTime to JDBC TIMESTAMP.</span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">Added support to map java.time.LocalOffsetTime to JDBC </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">TIME_WITH_TIMEZONE.</span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">Added support to map java.time.LocalOffsetDateTime to JDBC </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">TIMESTAMP_WITH_TIMEZONE.</span>
<span style="font-family:'ZapfDingbats';font-size:5.99pt;color:#000000">I</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> Additional Mappings to Table B-5, Performed by setObject and setNull between </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">Java Object Types and Target JDBC Types</span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">Allow conversion of java.time.LocalDate to CHAR, VARCHAR, </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">LONGVARCHAR, and DATE. </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">Allow conversion of java.time.LocalTime to CHAR, VARCHAR, </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">LONGVARCHAR, and TIME. </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">Allow conversion of java.time.LocalTime to CHAR, VARCHAR, </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">LONGVARCHAR, and TIMESTAMP.</span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">Allow conversion of java.time.OffsetTime to CHAR, VARCHAR, </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">LONGVARCHAR, and TIME_WITH_TIMESTAMP.</span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">Allow conversion of java.time.OffsetDateTime to CHAR, VARCHAR, </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">LONGVARCHAR, TIME_WITH_TIMESTAMP and </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">TIMESTAMP_WITH_TIMESTAMP.</span>
<span style="font-family:'ZapfDingbats';font-size:5.99pt;color:#000000">I</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> Additional Mappings to Table B-6, Use ResultSet getter Methods to retrieve JDBC </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">Types</span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">Allow getObject to return TIME_WITH_TIMEZONE, </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">TIMESTAMP_WITH_TIMEZONE.</span>
<span style="font-family:'ZapfDingbats';font-size:5.99pt;color:#000000">I</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> JDBC API changes</span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">The following changes were made to existing JDBC interfaces..</span>
<span style="font-family:'ZapfDingbats';font-size:4.92pt;color:#000000">I</span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">BatchUpdateException</span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">Added a new constructor to support large update counts.</span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">Added the method </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">getLargeUpdateCounts</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">.</span>
<span style="font-family:'ZapfDingbats';font-size:4.92pt;color:#000000">I</span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">Connection</span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">Added the methods </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">abort,getNetworkTimeout</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">, </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">getSchema</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">, </span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">setNetworkTimeout</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">, </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">setSchema</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">.</span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">Clarified the </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">getMapType, setSchema, setMapType </span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">methods.</span>
<span style="font-family:'ZapfDingbats';font-size:4.92pt;color:#000000">I</span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">CallableStatement</span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">Overloaded the </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">registerOutParameter</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> and </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">setObject </span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">methods.</span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">Clarified the </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">getObject</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> methods.</span>
<span style="font-family:'ZapfDingbats';font-size:4.92pt;color:#000000">I</span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">Date</span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">Added the methods toInstant, toLocalDate</span>

<span style="font-family:'Helvetica-Bold';font-size:7.91pt;font-weight:bold;color:#000000">12</span>
<span style="font-family:'Helvetica';font-size:7.91pt;color:#000000">JDBC 4.2 Specification • March 2014</span>

---

## Page 28

<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">Overload the method </span><span style="font-family:'Courier-Oblique';font-size:9.47pt;font-style:italic;color:#000000">valueOf</span>
<span style="font-family:'ZapfDingbats';font-size:4.92pt;color:#000000">I</span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">DatabaseMetaData</span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">Clarified the </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">getIndexInfo </span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">method.</span>
<span style="font-family:'ZapfDingbats';font-size:4.92pt;color:#000000">I</span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">Driver</span>
<span style="font-family:'ZapfDingbats';font-size:4.92pt;color:#000000">I</span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">DriverManager</span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">Overload the </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">registerDriver </span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">method.</span>
<span style="font-family:'ZapfDingbats';font-size:4.92pt;color:#000000">I</span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">PreparedStatement</span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">Added the method </span><span style="font-family:'Courier-Oblique';font-size:9.47pt;font-style:italic;color:#000000">executeLargeUpdate</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">.</span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">Overload the method </span><span style="font-family:'Courier-Oblique';font-size:9.47pt;font-style:italic;color:#000000">setObject</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">.</span>
<span style="font-family:'ZapfDingbats';font-size:4.92pt;color:#000000">I</span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">ResultSet</span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">Overloaded the methods </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">updateObject.</span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">Clarified the </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">getObject </span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">methods.</span>
<span style="font-family:'ZapfDingbats';font-size:4.92pt;color:#000000">I</span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">Statement</span>

<span style="font-family:'ZapfDingbats';font-size:4.92pt;color:#000000">I</span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">SQLInput</span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">Added the readObject method.</span>
<span style="font-family:'ZapfDingbats';font-size:4.92pt;color:#000000">I</span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">SQLOutput</span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">Added the readObject method</span>
<span style="font-family:'ZapfDingbats';font-size:4.92pt;color:#000000">I</span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">Time</span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">Added the methods toInstant, toLocalTime</span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">Overload the method </span><span style="font-family:'Palatino-Italic';font-size:9.47pt;font-style:italic;color:#000000">valueOf</span>
<span style="font-family:'ZapfDingbats';font-size:4.92pt;color:#000000">I</span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">Timestamp</span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">Overload the method </span><span style="font-family:'Palatino-Italic';font-size:9.47pt;font-style:italic;color:#000000">valueOf</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">.</span>
<span style="font-family:'ZapfDingbats';font-size:4.92pt;color:#000000">I</span>
<span style="font-family:'Courier-Oblique';font-size:9.47pt;font-style:italic;color:#000000">Types</span>
<span style="font-family:'ZapfDingbats';font-size:4.92pt;color:#000000">I</span>
<span style="font-family:'Courier-Oblique';font-size:9.47pt;font-style:italic;color:#000000">SQLXML</span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">Added the methods </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">supportsRefCursor,</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">getMaxLogicalLobSize</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">.</span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">Clarified the </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">acceptsURL</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">, and </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">connect,</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> methods</span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">Clarified the </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">getConnection</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">, </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">deregisterDriver</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">, and </span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">registerDriver</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">, methods.</span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">Added the method </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">executeLargeBatch</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">, </span><span style="font-family:'Courier-Oblique';font-size:9.47pt;font-style:italic;color:#000000">executeLargeUpdate</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">. </span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">getLargeUpdateCount</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">, </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">getLargeMaxRows</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">, and </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">setLargeMaxRows</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">.</span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">Clarified the </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">setEscapeProcessing </span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">method.</span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">Added the methods from, toInstant, toLocalTime</span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">Added the types </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">REF_CURSOR</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">,</span><span style="font-family:'Courier';font-size:9.47pt;color:#000000"> TIME_WITH_TIMEZONE</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">, and</span><span style="font-family:'Courier';font-size:9.47pt;color:#000000"> </span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">TIMESTAMP_WITH_TIEMZONE</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">.</span>

<span style="font-family:'Helvetica';font-size:7.91pt;color:#000000">Chapter 3</span>
<span style="font-family:'Helvetica';font-size:7.91pt;color:#000000">Summary of New Features</span>
<span style="font-family:'Helvetica-Bold';font-size:7.91pt;font-weight:bold;color:#000000">13</span>

---

## Page 29

<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">Clarified the </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">getSource </span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">and</span><span style="font-family:'Courier';font-size:9.47pt;color:#000000"> setResult </span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">methods.</span>
<span style="font-family:'ZapfDingbats';font-size:4.92pt;color:#000000">I</span>
<span style="font-family:'Courier-Oblique';font-size:9.47pt;font-style:italic;color:#000000">DataSource </span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">and</span><span style="font-family:'Courier';font-size:9.47pt;color:#000000"> XADataSource</span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">Clarified that a </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">no-arg </span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">constructor must be provided.</span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">See Chapter 5 “Classes and Interfaces” for a list of the classes and interfaces affected </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">by these changes.</span>

<span style="font-family:'Helvetica-Bold';font-size:7.91pt;font-weight:bold;color:#000000">14</span>
<span style="font-family:'Helvetica';font-size:7.91pt;color:#000000">JDBC 4.2 Specification • March 2014</span>

---

## Page 30

<span style="font-family:'Helvetica';font-size:8.39pt;color:#000000">CHAPTER</span><span style="font-family:'Helvetica-Bold';font-size:17.98pt;font-weight:bold;color:#000000"> 4</span>

<span style="font-family:'Palatino-Roman';font-size:20.98pt;color:#000000">Overview </span>

<span style="font-family:'Palatino-Roman';font-size:19.90pt;color:#000000">4.1</span>
<span style="font-family:'Palatino-Roman';font-size:19.90pt;color:#000000">Establishing a Connection</span>

<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">The JDBC API provides a way for Java programs to access one or more sources of </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">data. In the majority of cases, the data source is a relational DBMS, and its data is </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">accessed using SQL. However, it is also possible for JDBC technology-enabled </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">drivers to be implemented on top of other data sources, including legacy file systems </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">and object-oriented systems. A primary motivation for the JDBC API is to provide a </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">standard API for applications to access a wide variety of data sources. </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">This chapter introduces some of the key concepts of the JDBC API. In addition, it </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">describes two common environments for JDBC applications, with a discussion of </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">how different functional roles are implemented in each one. The two-tier and three-</span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">tier models are logical configurations that can be implemented on a variety of </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">physical configurations.</span>

<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">The JDBC API defines the </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">Connection</span><span style="font-family:'Palatino-Bold';font-size:9.47pt;font-weight:bold;color:#000000"> </span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">interface to represent a connection to an </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">underlying data source.</span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">In a typical scenario, a JDBC application will connect to a target data source using </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">one of two mechanisms:</span>
<span style="font-family:'ZapfDingbats';font-size:5.99pt;color:#000000">I</span><span style="font-family:'Courier';font-size:9.47pt;color:#000000"> DriverManager</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> — this fully implemented class was introduced in the original </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">JDBC 1.0 API. When an application first attempts to connect to a data source by </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">specifying a URL, </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">DriverManager</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> will automatically load any JDBC drivers </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">found within the CLASSPATH (any drivers that are pre-JDBC 4.0 must be </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">explicitly loaded by the application).</span>
<span style="font-family:'ZapfDingbats';font-size:5.99pt;color:#000000">I</span><span style="font-family:'Courier';font-size:9.47pt;color:#000000"> DataSource</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> — this interface was introduced in the JDBC 2.0 Optional Package </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">API. It is preferred over </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">DriverManager</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> because it allows details about the </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">underlying data source to be transparent to the application. A </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">DataSource</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">object’s properties are set so that it represents a particular data source. When its </span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">getConnection</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> method is invoked, the </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">DataSource</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> instance will return a </span>

<span style="font-family:'Helvetica-Bold';font-size:7.91pt;font-weight:bold;color:#000000">15</span>

---

## Page 31

<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">connection to that data source. An application can be directed to a different data </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">source by simply changing the </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">DataSource</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> object’s properties; no change in </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">application code is needed. Likewise, a </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">DataSource</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> implementation can be </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">changed without changing the application code that uses it.</span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">The JDBC API also defines two important extensions of the </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">DataSource</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> interface to </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">support enterprise applications. These extensions are the following two interfaces:</span>
<span style="font-family:'ZapfDingbats';font-size:4.92pt;color:#000000">I</span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">ConnectionPoolDataSource</span><span style="font-family:'Palatino-Bold';font-size:9.47pt;font-weight:bold;color:#000000"> — </span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">supports caching and reusing of physical </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">connections, which improves application performance and scalability </span>
<span style="font-family:'ZapfDingbats';font-size:4.92pt;color:#000000">I</span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">XADataSource</span><span style="font-family:'Palatino-Bold';font-size:9.47pt;font-weight:bold;color:#000000"> — </span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">provides connections that can participate in a distributed </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">transaction</span>

<span style="font-family:'Palatino-Roman';font-size:19.90pt;color:#000000">4.2</span>
<span style="font-family:'Palatino-Roman';font-size:19.90pt;color:#000000">Executing SQL Statements and </span>
<span style="font-family:'Palatino-Roman';font-size:19.90pt;color:#000000">Manipulating Results</span>

<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">Once a connection has been established, an application using the JDBC API can </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">execute queries and updates against the target data source. The JDBC API provides </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">access to the most commonly implemented features of SQL:2003. Because different </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">vendors vary in their level of support for these features, the JDBC API includes the </span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">DatabaseMetadata</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> interface. Applications can use this interface to determine </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">whether a particular feature is supported by the data source they are using. The </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">JDBC API also defines escape syntax to allow an application to access non-standard </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">vendor-specific features. The use of escape syntax has the advantage of giving JDBC </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">applications access to the same feature set as native applications and at the same </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">time maintaining the portability of the application.</span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">Applications use methods in the </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">Connection</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> interface to specify transaction </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">attributes and create </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">Statement</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">, </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">PreparedStatement</span><span style="font-family:'Palatino-Bold';font-size:9.47pt;font-weight:bold;color:#000000">, </span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">or</span><span style="font-family:'Palatino-Bold';font-size:9.47pt;font-weight:bold;color:#000000"> </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">CallableStatement</span><span style="font-family:'Palatino-Bold';font-size:9.47pt;font-weight:bold;color:#000000"> </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">objects. These statements are used to execute SQL statements and retrieve results. </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">The </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">ResultSet</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> interface encapsulates the results of an SQL query. Statements may </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">also be batched, allowing an application to submit multiple updates to a data source </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">as a single unit of execution. </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">The JDBC API extends the </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">ResultSet</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> interface with the </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">RowSet</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> interface, thereby </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">providing a container for tabular data that is much more versatile than a standard </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">result set. A </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">RowSet</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> object is a JavaBeans</span><span style="font-family:'Palatino-Roman';font-size:6.35pt;color:#000000">TM</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> component, and it may operate without </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">being connected to its data source. For example, a </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">RowSet</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> implementation can be </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">serializable and therefore sent across a network, which is particularly useful for </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">small-footprint clients that want to operate on tabular data without incurring the </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">overhead of a JDBC driver and data source connection. Another feature of a </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">RowSet</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">implementation is that it can include a custom reader for accessing any data in </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">tabular format, not just data in a relational database. Further, a </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">RowSet</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> object can </span>

<span style="font-family:'Helvetica-Bold';font-size:7.91pt;font-weight:bold;color:#000000">16</span>
<span style="font-family:'Helvetica';font-size:7.91pt;color:#000000">JDBC 4.2 Specification • March 2014</span>

---

## Page 32

<span style="font-family:'Palatino-Roman';font-size:15.94pt;color:#000000">4.2.1</span>
<span style="font-family:'Palatino-Roman';font-size:15.94pt;color:#000000">Support for SQL Advanced Data Types</span>

<span style="font-family:'Palatino-Roman';font-size:19.90pt;color:#000000">4.3</span>
<span style="font-family:'Palatino-Roman';font-size:19.90pt;color:#000000">Two-tier Model</span>

<span style="font-family:'Helvetica-Bold';font-size:6.95pt;font-weight:bold;color:#000000">FIGURE 4-1 </span><span style="font-family:'Palatino-Roman';font-size:8.99pt;color:#000000">Two-tier Model</span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">update its rows while it is disconnected from its data source, and its implementation </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">can include a custom writer that writes those updates back to the underlying data </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">source.</span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">The JDBC API defines standard mappings to convert SQL data types to JDBC data </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">types and back. This includes support for SQL:2003 advanced data types such as </span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">BLOB</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">, </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">CLOB</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">, </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">ARRAY</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">, </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">REF</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">, </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">STRUCT</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">, </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">XML</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">, and </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">DISTINCT</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">. JDBC drivers may also </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">implement one or more customized type mappings for user-defined types (UDTs), in </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">which the UDT is mapped to a class in the Java programming language. The JDBC </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">API also provides support for externally managed data, for example, data in a file </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">outside the data source.</span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">A two-tier model divides functionality into a client layer and a server layer, as </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">shown in </span><span style="font-family:'Palatino-Roman';font-size:7.91pt;color:#000000">FIGURE 4-1</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">.</span>

<span style="font-family:'Palatino-Bold';font-size:11.99pt;font-weight:bold;color:#000000">Application</span>
<span style="font-family:'Palatino-Bold';font-size:11.99pt;font-weight:bold;color:#000000">JDBC Driver</span>

<span style="font-family:'Palatino-Bold';font-size:11.99pt;font-weight:bold;color:#000000">data source</span>

<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">The client layer includes the application(s) and one or more JDBC drivers, with the </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">application handling these areas of responsibility:</span>

<span style="font-family:'Helvetica';font-size:7.91pt;color:#000000">Chapter 4</span>
<span style="font-family:'Helvetica';font-size:7.91pt;color:#000000">Overview</span>
<span style="font-family:'Helvetica-Bold';font-size:7.91pt;font-weight:bold;color:#000000">17</span>

---

## Page 33

<span style="font-family:'ZapfDingbats';font-size:5.99pt;color:#000000">I</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> presentation logic </span>
<span style="font-family:'ZapfDingbats';font-size:5.99pt;color:#000000">I</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> business logic</span>
<span style="font-family:'ZapfDingbats';font-size:5.99pt;color:#000000">I</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> transaction management for multiple-statement transactions or distributed </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">transactions</span>
<span style="font-family:'ZapfDingbats';font-size:5.99pt;color:#000000">I</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> resource management </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">In this model, the application interacts directly with the JDBC driver(s), including </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">establishing and managing the physical connection(s) and dealing with the details of </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">specific underlying data source implementations. The application may use its </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">knowledge of a specific implementation to take advantage of nonstandard features </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">or do performance tuning.</span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">Some drawbacks of this model include:</span>
<span style="font-family:'ZapfDingbats';font-size:5.99pt;color:#000000">I</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> mingling presentation and business logic with infrastructure and system-level </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">functions. This presents an obstacle to producing maintainable code with a well-</span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">defined architecture. </span>
<span style="font-family:'ZapfDingbats';font-size:5.99pt;color:#000000">I</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> making applications less portable because they are tuned to a particular database </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">implementation. Applications that require connections to multiple databases must </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">be aware of the differences between the different vendors’ implementations.</span>
<span style="font-family:'ZapfDingbats';font-size:5.99pt;color:#000000">I</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> limiting scalability. Typically, the application will hold onto one or more physical </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">database connections until it terminates, limiting the number of concurrent </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">applications that can be supported. In this model, issues of performance, </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">scalability and availability are handled by the JDBC driver and the corresponding </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">underlying data source. If an application deals with multiple drivers, it may also </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">need to be aware of the different ways in which each driver/data source pair </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">resolves these issues. </span>

<span style="font-family:'Palatino-Roman';font-size:19.90pt;color:#000000">4.4</span>
<span style="font-family:'Palatino-Roman';font-size:19.90pt;color:#000000">Three-tier Model</span>

<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">The three-tier model introduces a middle-tier server to house business logic and </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">infrastructure, as shown in </span><span style="font-family:'Palatino-Roman';font-size:7.91pt;color:#000000">FIGURE 4-2</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">.</span>

<span style="font-family:'Helvetica-Bold';font-size:7.91pt;font-weight:bold;color:#000000">18</span>
<span style="font-family:'Helvetica';font-size:7.91pt;color:#000000">JDBC 4.2 Specification • March 2014</span>

---

## Page 34

<span style="font-family:'Palatino-Bold';font-size:11.99pt;font-weight:bold;color:#000000">Middle-tier Server</span>
<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">Application</span>
<span style="font-family:'Palatino-Bold';font-size:8.99pt;font-weight:bold;color:#000000">Application</span>

<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">JDBC </span>
<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">Driver</span>
<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">data source</span>

<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">Web Client </span>
<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">(Browser)</span>
<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">Application </span>
<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">Server</span>

<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">JDBC </span>
<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">Driver</span>
<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">data source</span>

<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">transaction </span>
<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">manager</span>

<span style="font-family:'Helvetica-Bold';font-size:6.95pt;font-weight:bold;color:#000000">FIGURE 4-2 </span><span style="font-family:'Palatino-Roman';font-size:8.99pt;color:#000000">Three-tier Model</span>

<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">This architecture is designed to provide improved performance, scalability and </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">availability for enterprise applications. Functionality is divided among the tiers as </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">follows:</span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">1. Client tier — a thin layer implementing presentation logic for human interaction. </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">Java programs, web browsers and PDAs are typical client-tier implementations. </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">The client interacts with the middle-tier application and does not need to include </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">any knowledge of infrastructure or underlying data source functions. </span>
<span style="font-family:'ZapfDingbats';font-size:4.92pt;color:#000000">I</span>
<span style="font-family:'Palatino-BoldItalic';font-size:9.47pt;font-weight:bold;font-style:italic;color:#000000">Applications</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> to interact with the client and implement business logic. If the </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">application includes interaction with a data source, it will deal with higher-</span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">level abstractions, such as </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">DataSource</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> objects and logical connections rather </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">than lower-level driver API. </span>

<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">2. Middle-tier server — a middle tier that includes:</span>

<span style="font-family:'Helvetica';font-size:7.91pt;color:#000000">Chapter 4</span>
<span style="font-family:'Helvetica';font-size:7.91pt;color:#000000">Overview</span>
<span style="font-family:'Helvetica-Bold';font-size:7.91pt;font-weight:bold;color:#000000">19</span>

---

## Page 35

<span style="font-family:'ZapfDingbats';font-size:4.92pt;color:#000000">I</span>
<span style="font-family:'Palatino-BoldItalic';font-size:9.47pt;font-weight:bold;font-style:italic;color:#000000">An application server</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> to provide supporting infrastructure for a wide range of </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">applications. This can include management and pooling of physical </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">connections, transaction management, and the masking of differences between </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">different JDBC drivers. This last point makes it easier to write portable </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">applications. The application server role can be implemented by a Java EE </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">server. Application servers implement the higher-level abstractions used by </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">applications and interact directly with JDBC drivers.</span>
<span style="font-family:'ZapfDingbats';font-size:4.92pt;color:#000000">I</span>
<span style="font-family:'Palatino-BoldItalic';font-size:9.47pt;font-weight:bold;font-style:italic;color:#000000">JDBC driver(s)</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> to provide connectivity to the underlying data sources. Each </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">driver implements the standard JDBC API on top of whatever features are </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">supported by its underlying data source. The driver layer may mask </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">differences between standard SQL:2003 syntax and the native dialect </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">supported by the data source. If the data source is not a relational DBMS, the </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">driver implements the relational layer used by the application server.</span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">3. Underlying data source — the tier where the data resides. It can include relational </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">DBMSs, legacy file systems, object-oriented DBMSs, data warehouses, </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">spreadsheets, or other means of packaging and presenting data. The only </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">requirement is a corresponding driver that supports the JDBC API.</span>

<span style="font-family:'Palatino-Roman';font-size:19.90pt;color:#000000">4.5</span>
<span style="font-family:'Palatino-Roman';font-size:19.90pt;color:#000000">JDBC in the Java EE Platform</span>

<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">Java EE components, such as JavaServer</span><span style="font-family:'Palatino-Roman';font-size:6.35pt;color:#000000">TM</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> Pages, Servlets, and Enterprise Java </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">Beans</span><span style="font-family:'Palatino-Roman';font-size:7.55pt;color:#000000">TM</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> (EJB</span><span style="font-family:'Palatino-Roman';font-size:7.55pt;color:#000000">TM</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">) components, often require access to relational data and use the </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">JDBC API for this access. When Java EE components use the JDBC API, the container </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">manages their transactions and data sources. This means that Java EE component </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">developers do not directly use the JDBC API’s transaction and datasource </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">management facilities. See the Java EE Platform Specification for further details.</span>

<span style="font-family:'Helvetica-Bold';font-size:7.91pt;font-weight:bold;color:#000000">20</span>
<span style="font-family:'Helvetica';font-size:7.91pt;color:#000000">JDBC 4.2 Specification • March 2014</span>

---

## Page 36

<span style="font-family:'Helvetica';font-size:8.39pt;color:#000000">CHAPTER</span><span style="font-family:'Helvetica-Bold';font-size:17.98pt;font-weight:bold;color:#000000"> 5</span>

<span style="font-family:'Palatino-Roman';font-size:20.98pt;color:#000000">Classes and Interfaces</span>

<span style="font-family:'Palatino-Roman';font-size:19.90pt;color:#000000">5.1</span>
<span style="font-family:'Palatino-Roman';font-size:19.90pt;color:#000000">The </span><span style="font-family:'Courier';font-size:19.90pt;color:#000000">java.sql</span><span style="font-family:'Palatino-Roman';font-size:19.90pt;color:#000000"> Package</span>

<span style="font-family:'Courier';font-size:8.99pt;color:#000000">java.sql.Array</span>
<span style="font-family:'Courier-Bold';font-size:8.99pt;font-weight:bold;color:#000000">java.sql.BatchUpdateException </span>
<span style="font-family:'Courier';font-size:8.99pt;color:#000000">java.sql.Blob</span>
<span style="font-family:'Courier';font-size:8.99pt;color:#000000">java.sql.CallableStatement</span>
<span style="font-family:'Courier';font-size:8.99pt;color:#000000">java.sql.Clob</span>
<span style="font-family:'Courier';font-size:8.99pt;color:#000000">java.sql.ClientinfoStatus</span>
<span style="font-family:'Courier';font-size:8.99pt;color:#000000">java.sql.Connection</span>
<span style="font-family:'Courier-Bold';font-size:8.99pt;font-weight:bold;color:#000000">java.sql.DataTruncation </span>
<span style="font-family:'Courier';font-size:8.99pt;color:#000000">java.sql.DatabaseMetaData</span>
<span style="font-family:'Courier-Bold';font-size:8.99pt;font-weight:bold;color:#000000">java.sql.Date </span>
<span style="font-family:'Courier';font-size:8.99pt;color:#000000">java.sql.Driver</span>
<span style="font-family:'Courier';font-size:8.99pt;color:#000000">java.sql.DriverAction</span>
<span style="font-family:'Courier-Bold';font-size:8.99pt;font-weight:bold;color:#000000">java.sql.DriverManager </span>
<span style="font-family:'Courier-Bold';font-size:8.99pt;font-weight:bold;color:#000000">java.sql.DriverPropertyInfo</span>
<span style="font-family:'Courier-Bold';font-size:8.99pt;font-weight:bold;color:#000000">java.sql.JDBCType</span>
<span style="font-family:'Courier';font-size:8.99pt;color:#000000">java.sql.NClob</span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">The following classes and interfaces make up the JDBC API. </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">The core JDBC API is contained in the package </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">java.sql</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">. The enums, classes and </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">interfaces in </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">java.sql</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> are listed below. Enums and classes are bold type; interfaces </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">are in standard type. </span>

<span style="font-family:'Helvetica-Bold';font-size:7.91pt;font-weight:bold;color:#000000">21</span>

---

## Page 37

<span style="font-family:'Courier';font-size:8.99pt;color:#000000">java.sql.ParameterMetaData</span>
<span style="font-family:'Courier';font-size:8.99pt;color:#000000">java.sql.PreparedStatement</span>
<span style="font-family:'Courier-Bold';font-size:8.99pt;font-weight:bold;color:#000000">java.sql.PseudoColumnUsage</span>
<span style="font-family:'Courier';font-size:8.99pt;color:#000000">java.sql.Ref</span>
<span style="font-family:'Courier';font-size:8.99pt;color:#000000">java.sql.ResultSet</span>
<span style="font-family:'Courier';font-size:8.99pt;color:#000000">java.sql.ResultSetMetaData</span>
<span style="font-family:'Courier';font-size:8.99pt;color:#000000">java.sql.RowId</span>
<span style="font-family:'Courier-Bold';font-size:8.99pt;font-weight:bold;color:#000000">java.sql.RowIdLifeTime</span>
<span style="font-family:'Courier';font-size:8.99pt;color:#000000">java.sql.Savepoint</span>
<span style="font-family:'Courier-Bold';font-size:8.99pt;font-weight:bold;color:#000000">java.sql.SQLClientInfoException</span>
<span style="font-family:'Courier';font-size:8.99pt;color:#000000">java.sql.SQLData</span>
<span style="font-family:'Courier-Bold';font-size:8.99pt;font-weight:bold;color:#000000">java.sql.SQLDataException</span>
<span style="font-family:'Courier-Bold';font-size:8.99pt;font-weight:bold;color:#000000">java.sql.SQLException </span>
<span style="font-family:'Courier-Bold';font-size:8.99pt;font-weight:bold;color:#000000">java.sql.SQLFeatureNotSupportedException </span>
<span style="font-family:'Courier';font-size:8.99pt;color:#000000">java.sql.SQLInput</span>
<span style="font-family:'Courier-Bold';font-size:8.99pt;font-weight:bold;color:#000000">java.sql.SQLIntegrityConstraintViolationException </span>
<span style="font-family:'Courier-Bold';font-size:8.99pt;font-weight:bold;color:#000000">java.sql.SQLInvalidAuthorizationSpecException </span>
<span style="font-family:'Courier-Bold';font-size:8.99pt;font-weight:bold;color:#000000">java.sql.SQLNonTransientConnectionException </span>
<span style="font-family:'Courier-Bold';font-size:8.99pt;font-weight:bold;color:#000000">java.sql.SQLNonTransientException </span>
<span style="font-family:'Courier';font-size:8.99pt;color:#000000">java.sql.SQLOutput</span>
<span style="font-family:'Courier';font-size:8.99pt;color:#000000">java.sql.SQLPermission</span>
<span style="font-family:'Courier-Bold';font-size:8.99pt;font-weight:bold;color:#000000">java.sql.SQLSyntaxErrorException </span>
<span style="font-family:'Courier-Bold';font-size:8.99pt;font-weight:bold;color:#000000">java.sql.SQLTimeoutException </span>
<span style="font-family:'Courier-Bold';font-size:8.99pt;font-weight:bold;color:#000000">java.sql.SQLTransactionRollbackException </span>
<span style="font-family:'Courier-Bold';font-size:8.99pt;font-weight:bold;color:#000000">java.sql.SQLTransientConnectionException </span>
<span style="font-family:'Courier-Bold';font-size:8.99pt;font-weight:bold;color:#000000">java.sql.SQLTransientException</span>
<span style="font-family:'Courier';font-size:8.99pt;color:#000000">java.sql.SQLType</span>
<span style="font-family:'Courier';font-size:8.99pt;color:#000000">java.sql.SQLXML</span><span style="font-family:'Courier-Bold';font-size:8.99pt;font-weight:bold;color:#000000"> </span>
<span style="font-family:'Courier-Bold';font-size:8.99pt;font-weight:bold;color:#000000">java.sql.SQLWarning </span>
<span style="font-family:'Courier';font-size:8.99pt;color:#000000">java.sql.Statement</span>
<span style="font-family:'Courier';font-size:8.99pt;color:#000000">java.sql.Struct</span>
<span style="font-family:'Courier-Bold';font-size:8.99pt;font-weight:bold;color:#000000">java.sql.Time </span>
<span style="font-family:'Courier-Bold';font-size:8.99pt;font-weight:bold;color:#000000">java.sql.Timestamp </span>
<span style="font-family:'Courier-Bold';font-size:8.99pt;font-weight:bold;color:#000000">java.sql.Types</span>
<span style="font-family:'Courier';font-size:8.99pt;color:#000000">java.sql.Wrapper</span>

<span style="font-family:'Helvetica-Bold';font-size:7.91pt;font-weight:bold;color:#000000">22</span>
<span style="font-family:'Helvetica';font-size:7.91pt;color:#000000">JDBC 4.2 Specification • March 2014</span>

---

## Page 38

<span style="font-family:'Courier';font-size:8.99pt;color:#000000">java.sql.BatchUpdateException</span>
<span style="font-family:'Courier';font-size:8.99pt;color:#000000">java.sql.CallableStatement</span>
<span style="font-family:'Courier';font-size:8.99pt;color:#000000">java.sql.Connection</span>
<span style="font-family:'Courier';font-size:8.99pt;color:#000000">java.sql.DatabaseMetaData</span>
<span style="font-family:'Courier';font-size:8.99pt;color:#000000">java.sql.Date</span>
<span style="font-family:'Courier';font-size:8.99pt;color:#000000">java.sql.Driver</span>
<span style="font-family:'Courier-Bold';font-size:8.99pt;font-weight:bold;color:#000000">java.sql.DriverAction</span>
<span style="font-family:'Courier';font-size:8.99pt;color:#000000">java.sql.DriverManager</span>
<span style="font-family:'Courier-Bold';font-size:8.99pt;font-weight:bold;color:#000000">java.sql.JDBCType</span>
<span style="font-family:'Courier';font-size:8.99pt;color:#000000">java.sql.Permission</span>
<span style="font-family:'Courier';font-size:8.99pt;color:#000000">java.sql.PreparedStatement</span>
<span style="font-family:'Courier';font-size:8.99pt;color:#000000">java.sql.ResultSet</span>
<span style="font-family:'Courier';font-size:8.99pt;color:#000000">java.sql.SQLInput</span>
<span style="font-family:'Courier';font-size:8.99pt;color:#000000">java.sql.SQLOutput</span>
<span style="font-family:'Courier-Bold';font-size:8.99pt;font-weight:bold;color:#000000">java.sql.SQLType</span>
<span style="font-family:'Courier';font-size:8.99pt;color:#000000">java.sql.SQLXML</span>
<span style="font-family:'Courier';font-size:8.99pt;color:#000000">java.sql.Statement</span>
<span style="font-family:'Courier';font-size:8.99pt;color:#000000">java.sql.Types</span>
<span style="font-family:'Courier';font-size:8.99pt;color:#000000">java.sql.Timestamp</span>
<span style="font-family:'Courier';font-size:8.99pt;color:#000000">javax.sql.XADataSource</span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">The following classes and interfaces are either new or updated in the JDBC 4.2 API. </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">New classes and interfaces are highlighted in bold.</span>
<span style="font-family:'Palatino-Roman';font-size:7.91pt;color:#000000">FIGURE 5-1</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> shows the interactions and relationships between the key classes and </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">interfaces in the </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">java.sql</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> package. The methods involved in creating statements, </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">setting parameters and retrieving results are also shown.</span>

<span style="font-family:'Helvetica';font-size:7.91pt;color:#000000">Chapter 5</span>
<span style="font-family:'Helvetica';font-size:7.91pt;color:#000000">Classes and Interfaces</span>
<span style="font-family:'Helvetica-Bold';font-size:7.91pt;font-weight:bold;color:#000000">23</span>

---

## Page 39

<span style="font-family:'Palatino-Bold';font-size:7.91pt;font-weight:bold;color:#000000">Connection</span>

<span style="font-family:'Palatino-Bold';font-size:7.91pt;font-weight:bold;color:#000000">createStatement</span>
<span style="font-family:'Palatino-Bold';font-size:8.29pt;font-weight:bold;color:#000000">prepareStatement</span>
<span style="font-family:'Palatino-Bold';font-size:7.91pt;font-weight:bold;color:#000000">prepareCall</span>

<span style="font-family:'Palatino-Bold';font-size:7.91pt;font-weight:bold;color:#000000">Statement</span>
<span style="font-family:'Palatino-Bold';font-size:7.91pt;font-weight:bold;color:#000000">subclasses</span>
<span style="font-family:'Palatino-Bold';font-size:7.91pt;font-weight:bold;color:#000000">PreparedStatement</span>
<span style="font-family:'Palatino-Bold';font-size:7.91pt;font-weight:bold;color:#000000">subclasses</span>
<span style="font-family:'Palatino-Bold';font-size:7.91pt;font-weight:bold;color:#000000">CallableStatement</span>
<span style="font-family:'Palatino-Bold';font-size:7.91pt;font-weight:bold;color:#000000">Input/Output of</span>

<span style="font-family:'Palatino-Bold';font-size:7.91pt;font-weight:bold;color:#000000">PreparedStatement</span>
<span style="font-family:'Palatino-Bold';font-size:7.91pt;font-weight:bold;color:#000000">CallableStatement</span>
<span style="font-family:'Palatino-Bold';font-size:7.91pt;font-weight:bold;color:#000000">Input to</span>

<span style="font-family:'Palatino-Bold';font-size:7.91pt;font-weight:bold;color:#000000">executeQuery</span>
<span style="font-family:'Palatino-Bold';font-size:7.91pt;font-weight:bold;color:#000000">Data types</span>
<span style="font-family:'Palatino-Bold';font-size:7.91pt;font-weight:bold;color:#000000">getMoreResults / getResultSet</span>

<span style="font-family:'Palatino-Bold';font-size:7.91pt;font-weight:bold;color:#000000">executeQuery</span>
<span style="font-family:'Palatino-Bold';font-size:7.91pt;font-weight:bold;color:#000000">getXXX</span>
<span style="font-family:'Palatino-Bold';font-size:7.91pt;font-weight:bold;color:#000000">executeQuery</span>

<span style="font-family:'Palatino-Bold';font-size:7.91pt;font-weight:bold;color:#000000">ResultSet</span>

<span style="font-family:'Helvetica-Bold';font-size:6.95pt;font-weight:bold;color:#000000">FIGURE 5-1 </span><span style="font-family:'Palatino-Roman';font-size:8.99pt;color:#000000">Relationships between major classes and interface in the </span><span style="font-family:'Courier';font-size:8.99pt;color:#000000">java.sql</span><span style="font-family:'Palatino-Roman';font-size:8.99pt;color:#000000"> package</span>

<span style="font-family:'Helvetica-Bold';font-size:7.91pt;font-weight:bold;color:#000000">24</span>
<span style="font-family:'Helvetica';font-size:7.91pt;color:#000000">JDBC 4.2 Specification • March 2014</span>

---

## Page 40

<span style="font-family:'Palatino-Roman';font-size:19.90pt;color:#000000">5.2</span>
<span style="font-family:'Palatino-Roman';font-size:19.90pt;color:#000000">The </span><span style="font-family:'Courier';font-size:19.90pt;color:#000000">javax.sql</span><span style="font-family:'Palatino-Roman';font-size:19.90pt;color:#000000"> Package</span>

<span style="font-family:'Courier';font-size:8.99pt;color:#000000">javax.sql.CommonDataSource</span>
<span style="font-family:'Courier-Bold';font-size:8.99pt;font-weight:bold;color:#000000">javax.sql.ConnectionEvent </span>
<span style="font-family:'Courier';font-size:8.99pt;color:#000000">javax.sql.ConnectionEventListener</span>
<span style="font-family:'Courier';font-size:8.99pt;color:#000000">javax.sql.ConnectionPoolDataSource</span>
<span style="font-family:'Courier';font-size:8.99pt;color:#000000">javax.sql.DataSource</span>
<span style="font-family:'Courier';font-size:8.99pt;color:#000000">javax.sql.PooledConnection</span>
<span style="font-family:'Courier';font-size:8.99pt;color:#000000">javax.sql.RowSet</span>
<span style="font-family:'Courier-Bold';font-size:8.99pt;font-weight:bold;color:#000000">javax.sql.RowSetEvent </span>
<span style="font-family:'Courier';font-size:8.99pt;color:#000000">javax.sql.RowSetInternal</span>
<span style="font-family:'Courier';font-size:8.99pt;color:#000000">javax.sql.RowSetListener</span>
<span style="font-family:'Courier';font-size:8.99pt;color:#000000">javax.sql.RowSetMetaData</span>
<span style="font-family:'Courier';font-size:8.99pt;color:#000000">javax.sql.RowSetReader</span>
<span style="font-family:'Courier';font-size:8.99pt;color:#000000">javax.sql.RowSetWriter</span>
<span style="font-family:'Courier-Bold';font-size:8.99pt;font-weight:bold;color:#000000">javax.sql.StatementEvent</span>
<span style="font-family:'Courier';font-size:8.99pt;color:#000000">javax.sql.StatementEventListener</span>
<span style="font-family:'Courier';font-size:8.99pt;color:#000000">javax.sql.XAConnection</span>
<span style="font-family:'Courier';font-size:8.99pt;color:#000000">javax.sql.XADataSource</span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">The following list contains the classes and interfaces that are contained in the </span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">javax.sql</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> package. Classes are highlighted in bold; interfaces are in normal type.</span>
<span style="font-family:'Helvetica-Bold';font-size:9.95pt;font-weight:bold;color:#000000">Note – </span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">The classes and interfaces in the </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">javax.sql</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> package were first made </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">available as the JDBC 2.0 Optional Package. This optional package was previously </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">separate from the </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">java.sql</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> package, which was part of J2SE 1.2. Both packages </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">(</span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">java.sql</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> and </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">javax.sql</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">) are now part of Java SE as of J2SE 1.4. </span>
<span style="font-family:'Palatino-Roman';font-size:7.91pt;color:#000000">FIGURE 5-2</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">, </span><span style="font-family:'Palatino-Roman';font-size:7.91pt;color:#000000">FIGURE 5-3</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">, </span><span style="font-family:'Palatino-Roman';font-size:7.91pt;color:#000000">FIGURE 5-4</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">, and </span><span style="font-family:'Palatino-Roman';font-size:7.91pt;color:#000000">FIGURE 5-5</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> show the relationships between key </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">classes and interfaces in these areas of functionality: </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">DataSource</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> objects, </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">connection pooling, distributed transactions, and rowsets.</span>

<span style="font-family:'Helvetica';font-size:7.91pt;color:#000000">Chapter 5</span>
<span style="font-family:'Helvetica';font-size:7.91pt;color:#000000">Classes and Interfaces</span>
<span style="font-family:'Helvetica-Bold';font-size:7.91pt;font-weight:bold;color:#000000">25</span>

---

## Page 41

<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">java.sql</span>
<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">javax.sql</span>

<span style="font-family:'Palatino-Bold';font-size:7.91pt;font-weight:bold;color:#000000">DataSource</span>
<span style="font-family:'Palatino-Bold';font-size:7.91pt;font-weight:bold;color:#000000">Connection</span>
<span style="font-family:'Palatino-Bold';font-size:7.91pt;font-weight:bold;color:#000000">getConnection</span>

<span style="font-family:'Helvetica-Bold';font-size:6.95pt;font-weight:bold;color:#000000">FIGURE 5-2 </span><span style="font-family:'Palatino-Roman';font-size:8.99pt;color:#000000">Relationship between </span><span style="font-family:'Courier';font-size:8.99pt;color:#000000">javax.sql.DataSource</span><span style="font-family:'Palatino-Roman';font-size:8.99pt;color:#000000"> and </span>
<span style="font-family:'Courier';font-size:8.99pt;color:#000000">java.sql.Connection</span>

<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">javax.sql</span>
<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">java.sql</span>

<span style="font-family:'Palatino-Bold';font-size:7.91pt;font-weight:bold;color:#000000">ConnectionPoolDataSource</span>

<span style="font-family:'Palatino-Bold';font-size:7.91pt;font-weight:bold;color:#000000">getConnection</span>
<span style="font-family:'Palatino-Bold';font-size:7.91pt;font-weight:bold;color:#000000">close or error event</span>

<span style="font-family:'Palatino-Bold';font-size:7.91pt;font-weight:bold;color:#000000">Connection</span>
<span style="font-family:'Palatino-Bold';font-size:7.91pt;font-weight:bold;color:#000000">PooledConnection</span>
<span style="font-family:'Palatino-Bold';font-size:7.91pt;font-weight:bold;color:#000000">getConnection</span>

<span style="font-family:'Palatino-Bold';font-size:7.91pt;font-weight:bold;color:#000000">ConnectionEvent</span>
<span style="font-family:'Palatino-Bold';font-size:7.91pt;font-weight:bold;color:#000000">ConnectionEventListener</span>

<span style="font-family:'Helvetica-Bold';font-size:6.95pt;font-weight:bold;color:#000000">FIGURE 5-3 </span><span style="font-family:'Palatino-Roman';font-size:8.99pt;color:#000000">Relationships involved in connection pooling</span>

<span style="font-family:'Helvetica-Bold';font-size:7.91pt;font-weight:bold;color:#000000">26</span>
<span style="font-family:'Helvetica';font-size:7.91pt;color:#000000">JDBC 4.2 Specification • March 2014</span>

---

## Page 42

<span style="font-family:'Palatino-Bold';font-size:7.91pt;font-weight:bold;color:#000000">XAResource</span>
<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">java.sql</span>
<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">javax.sql</span>
<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">javax.transaction.xa</span>

<span style="font-family:'Palatino-Bold';font-size:7.91pt;font-weight:bold;color:#000000">PooledConnection</span>
<span style="font-family:'Palatino-Bold';font-size:7.91pt;font-weight:bold;color:#000000">subclasses</span>
<span style="font-family:'Palatino-Bold';font-size:7.91pt;font-weight:bold;color:#000000">getXAResource</span>
<span style="font-family:'Palatino-Bold';font-size:7.91pt;font-weight:bold;color:#000000">XADataSource</span>
<span style="font-family:'Palatino-Bold';font-size:7.91pt;font-weight:bold;color:#000000">getXAConnection</span>
<span style="font-family:'Palatino-Bold';font-size:7.91pt;font-weight:bold;color:#000000">XAConnection</span>

<span style="font-family:'Palatino-Bold';font-size:7.91pt;font-weight:bold;color:#000000">getConnection</span>
<span style="font-family:'Palatino-Bold';font-size:7.91pt;font-weight:bold;color:#000000">Connection</span>

<span style="font-family:'Palatino-Bold';font-size:7.91pt;font-weight:bold;color:#000000">close or error event</span>
<span style="font-family:'Palatino-Bold';font-size:7.91pt;font-weight:bold;color:#000000">ConnectionEvent</span>

<span style="font-family:'Palatino-Bold';font-size:7.91pt;font-weight:bold;color:#000000">ConnectionEventListener</span>

<span style="font-family:'Helvetica-Bold';font-size:6.95pt;font-weight:bold;color:#000000">FIGURE 5-4 </span><span style="font-family:'Palatino-Roman';font-size:8.99pt;color:#000000">Relationships involved in distributed transaction support</span>

<span style="font-family:'Helvetica';font-size:7.91pt;color:#000000">Chapter 5</span>
<span style="font-family:'Helvetica';font-size:7.91pt;color:#000000">Classes and Interfaces</span>
<span style="font-family:'Helvetica-Bold';font-size:7.91pt;font-weight:bold;color:#000000">27</span>

---

## Page 43

<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">javax.sql</span>
<span style="font-family:'Palatino-Bold';font-size:9.95pt;font-weight:bold;color:#000000">java.sql</span>
<span style="font-family:'Palatino-Bold';font-size:7.91pt;font-weight:bold;color:#000000">ResultSet</span>
<span style="font-family:'Palatino-Bold';font-size:7.91pt;font-weight:bold;color:#000000">subclasses</span>

<span style="font-family:'Palatino-Bold';font-size:7.91pt;font-weight:bold;color:#000000">RowSet</span>
<span style="font-family:'Palatino-Bold';font-size:7.91pt;font-weight:bold;color:#000000">RowSetEvent</span>
<span style="font-family:'Palatino-Bold';font-size:7.91pt;font-weight:bold;color:#000000">RowSetEventListener</span>

<span style="font-family:'Palatino-Bold';font-size:7.91pt;font-weight:bold;color:#000000">RowSetInternal</span>

<span style="font-family:'Palatino-Bold';font-size:7.91pt;font-weight:bold;color:#000000">ResultSetMetaData</span>
<span style="font-family:'Palatino-Bold';font-size:7.91pt;font-weight:bold;color:#000000">retrieves </span>

<span style="font-family:'Palatino-Bold';font-size:7.91pt;font-weight:bold;color:#000000">metadata</span>
<span style="font-family:'Palatino-Bold';font-size:7.91pt;font-weight:bold;color:#000000">reads data</span>
<span style="font-family:'Palatino-Bold';font-size:7.91pt;font-weight:bold;color:#000000">writes data</span>
<span style="font-family:'Palatino-Bold';font-size:7.91pt;font-weight:bold;color:#000000">RowSetMetaData</span>
<span style="font-family:'Palatino-Bold';font-size:7.91pt;font-weight:bold;color:#000000">RowSetWriter</span>

<span style="font-family:'Palatino-Bold';font-size:7.91pt;font-weight:bold;color:#000000">subclasses</span>
<span style="font-family:'Palatino-Bold';font-size:7.91pt;font-weight:bold;color:#000000">RowSetReader</span>

<span style="font-family:'Helvetica-Bold';font-size:6.95pt;font-weight:bold;color:#000000">FIGURE 5-5 </span><span style="font-family:'Courier';font-size:8.99pt;color:#000000">RowSet</span><span style="font-family:'Palatino-Roman';font-size:8.99pt;color:#000000"> relationships</span>

<span style="font-family:'Helvetica-Bold';font-size:7.91pt;font-weight:bold;color:#000000">28</span>
<span style="font-family:'Helvetica';font-size:7.91pt;color:#000000">JDBC 4.2 Specification • March 2014</span>

---

## Page 44

<span style="font-family:'Helvetica';font-size:8.39pt;color:#000000">CHAPTER</span><span style="font-family:'Helvetica-Bold';font-size:17.98pt;font-weight:bold;color:#000000"> 6</span>

<span style="font-family:'Palatino-Roman';font-size:20.98pt;color:#000000">Compliance</span>

<span style="font-family:'Palatino-Roman';font-size:19.90pt;color:#000000">6.1</span>
<span style="font-family:'Palatino-Roman';font-size:19.90pt;color:#000000">Definitions</span>

<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">This chapter identifies the features that a JDBC driver implementation is required to </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">support to claim compliance. Any features not identified are considered optional for </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">compliance. </span>

<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">To avoid ambiguity, we will use these terms in our discussion of compliance:</span>
<span style="font-family:'ZapfDingbats';font-size:5.99pt;color:#000000">I</span><span style="font-family:'Palatino-Bold';font-size:9.47pt;font-weight:bold;color:#000000"> JDBC driver implementation</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> — a JDBC technology-enabled driver and its </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">underlying data source. The driver may provide support for features that are not </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">implemented by the underlying data source. It may also provide the mapping </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">between standard syntax/semantics and the native API implemented by the data </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">source. </span>
<span style="font-family:'ZapfDingbats';font-size:5.99pt;color:#000000">I</span><span style="font-family:'Palatino-Bold';font-size:9.47pt;font-weight:bold;color:#000000"> Relevant specifications</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> — this document, the API specification, and the relevant </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">SQL specification. This is also the order of precedence if a feature is described in </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">more than one of these documents. For the JDBC API, it is SQL92 plus the </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">relevant sections of SQL:2003 and X/Open SQL CLI.</span>
<span style="font-family:'ZapfDingbats';font-size:5.99pt;color:#000000">I</span><span style="font-family:'Palatino-Bold';font-size:9.47pt;font-weight:bold;color:#000000"> Supported feature</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> — a feature for which the JDBC API implementation supports </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">standard syntax and semantics for that feature as defined in the relevant </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">specifications.</span>
<span style="font-family:'ZapfDingbats';font-size:5.99pt;color:#000000">I</span><span style="font-family:'Palatino-Bold';font-size:9.47pt;font-weight:bold;color:#000000"> Partially Supported Feature</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">—A feature for which some methods are </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">implemented via standard syntax and semantics and some required methods </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">throw </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">SQLFeatureNotSupportedException</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> to indicate that it is not </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">supported.</span>
<span style="font-family:'ZapfDingbats';font-size:5.99pt;color:#000000">I</span><span style="font-family:'Palatino-Bold';font-size:9.47pt;font-weight:bold;color:#000000"> Extension</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> — a feature that is not covered by any of the relevant specifications or </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">a non-standard implementation of a feature that is covered.</span>
<span style="font-family:'ZapfDingbats';font-size:5.99pt;color:#000000">I</span><span style="font-family:'Palatino-Bold';font-size:9.47pt;font-weight:bold;color:#000000"> Fully implemented</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> — a term applied to an interface that has all of its methods </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">implemented to support the semantics defined in the relevant specifications. </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">None of the methods may throw an exception because they are not implemented.</span>

<span style="font-family:'Helvetica-Bold';font-size:7.91pt;font-weight:bold;color:#000000">29</span>

---

## Page 45

<span style="font-family:'ZapfDingbats';font-size:5.99pt;color:#000000">I</span><span style="font-family:'Palatino-Bold';font-size:9.47pt;font-weight:bold;color:#000000"> Must implement </span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">— an interface that must be implemented although some </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">methods on the interface are considered optional. Methods that are not </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">implemented must throw an </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">SQLFeatureNotSupportedException</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> to indicate </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">that the corresponding feature is not supported.</span>

<span style="font-family:'Palatino-Roman';font-size:19.90pt;color:#000000">6.2</span>
<span style="font-family:'Palatino-Roman';font-size:19.90pt;color:#000000">Guidelines and Requirements </span>

<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">The following guidelines apply to JDBC compliance:</span>
<span style="font-family:'ZapfDingbats';font-size:5.99pt;color:#000000">I</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> A JDBC API implementation must support Entry Level SQL92 plus the SQL </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">command </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">Drop Table</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> (see note.)</span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">Entry Level SQL92 represents a &quot;floor&quot; for the level of SQL that a JDBC API </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">implementation must support. Access to features based on SQL99 or SQL:2003 </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">should be provided in a way that is compatible with the relevant part of the </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">SQL99 or SQL:2003 specification.</span>
<span style="font-family:'ZapfDingbats';font-size:5.99pt;color:#000000">I</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> Drivers must support escape syntax. Escape syntax is described in Chapter 13 </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">“Statements”.</span>
<span style="font-family:'ZapfDingbats';font-size:5.99pt;color:#000000">I</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> Drivers must support transactions. See Chapter 10 “Transactions” for details.</span>
<span style="font-family:'ZapfDingbats';font-size:5.99pt;color:#000000">I</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> If a </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">DatabaseMetaData</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> method indicates that a given feature is supported, it </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">must be supported via standard syntax and semantics as described in the relevant </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">specifications and meet the requirements outlined in “JDBC 4.2 API Compliance” </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">on page 31. This may require the driver to provide the mapping to the data </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">source’s native API or SQL dialect if it differs from the standard.</span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">If a feature is supported, all of the relevant metadata methods must be </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">implemented. For example, if a JDBC API implementation supports the </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">RowSet</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">interface, it must also implement the </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">RowSetMetaData</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> interface.</span>
<span style="font-family:'ZapfDingbats';font-size:5.99pt;color:#000000">I</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> Drivers should provide access to every feature implemented by the underlying </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">data source, including features that extend the JDBC API. The intent is for </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">applications using the JDBC API to have access to the same feature set as native </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">applications.</span>
<span style="font-family:'ZapfDingbats';font-size:5.99pt;color:#000000">I</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> If a JDBC driver does not support or only provides partial support for an optional </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">feature, the corresponding </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">DatabaseMetaData</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> method must indicate the feature </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">is not supported. Any methods for a feature that is not implemented are required </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">to throw a </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">SQLFeatureNotSupportedException</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">.</span>

<span style="font-family:'Helvetica-Bold';font-size:7.91pt;font-weight:bold;color:#000000">30</span>
<span style="font-family:'Helvetica';font-size:7.91pt;color:#000000">JDBC 4.2 Specification • March 2014</span>

---

## Page 46

<span style="font-family:'Palatino-Roman';font-size:19.90pt;color:#000000">6.3</span>
<span style="font-family:'Palatino-Roman';font-size:19.90pt;color:#000000">JDBC 4.2 API Compliance</span>

<span style="font-family:'ZapfDingbats';font-size:5.99pt;color:#000000">I</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> Adhere to the preceding guidelines and requirements</span>
<span style="font-family:'ZapfDingbats';font-size:5.99pt;color:#000000">I</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> Support a </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">ResultSet</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> type of </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">TYPE_FORWARD_ONLY</span>
<span style="font-family:'ZapfDingbats';font-size:5.99pt;color:#000000">I</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> Support batch updates </span>
<span style="font-family:'ZapfDingbats';font-size:5.99pt;color:#000000">I</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> Fully implement the following interfaces: </span>
<span style="font-family:'ZapfDingbats';font-size:4.92pt;color:#000000">I</span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">java.sql.DatabaseMetaData</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> </span>
<span style="font-family:'ZapfDingbats';font-size:4.92pt;color:#000000">I</span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">java.sql.ParameterMetaData</span>
<span style="font-family:'ZapfDingbats';font-size:4.92pt;color:#000000">I</span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">java.sql.ResultSetMetaData</span>
<span style="font-family:'ZapfDingbats';font-size:4.92pt;color:#000000">I</span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">java.sql.Wrapper</span>
<span style="font-family:'ZapfDingbats';font-size:4.92pt;color:#000000">I</span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">getParentLogger</span>
<span style="font-family:'ZapfDingbats';font-size:4.92pt;color:#000000">I</span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">getParentLogger</span>

<span style="font-family:'ZapfDingbats';font-size:4.92pt;color:#000000">I</span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">getNetworkTimeout</span>
<span style="font-family:'Helvetica-Bold';font-size:9.95pt;font-weight:bold;color:#000000">Note – </span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">A JDBC API implementation is required to support the </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">DROP TABLE</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">command as specified by SQL92, Transitional Level. However, support for the </span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">CASCADE</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> and </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">RESTRICT</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> options of </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">DROP TABLE</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> is optional. In addition, the </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">behavior of </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">DROP TABLE</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> is implementation-defined when there are views or </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">integrity constraints defined that reference the table being dropped.</span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">A driver that is compliant with the JDBC specification must do the following:</span>
<span style="font-family:'ZapfDingbats';font-size:5.99pt;color:#000000">I</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> Support auto loading of the drivers </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">java.sql.Driver</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> implementation</span>
<span style="font-family:'ZapfDingbats';font-size:5.99pt;color:#000000">I</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> Support a </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">ResultSet</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> concurrency of </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">CONCUR_READ_ONLY</span>
<span style="font-family:'ZapfDingbats';font-size:5.99pt;color:#000000">I</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> It must implement the </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">DataSource</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> interface with the exception of the following </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">optional methods:</span>
<span style="font-family:'ZapfDingbats';font-size:5.99pt;color:#000000">I</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> It must implement the </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">Driver</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> interface with the exception of the following </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">optional methods:</span>
<span style="font-family:'ZapfDingbats';font-size:5.99pt;color:#000000">I</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> It must implement the </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">Connection</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> interface with the exception of the following </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">optional methods:</span>
<span style="font-family:'ZapfDingbats';font-size:4.92pt;color:#000000">I</span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">createArrayOf </span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">unless the driver supports the associated data type</span>
<span style="font-family:'ZapfDingbats';font-size:4.92pt;color:#000000">I</span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">createBlob </span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">unless the driver supports the associated data type</span>
<span style="font-family:'ZapfDingbats';font-size:4.92pt;color:#000000">I</span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">createClob </span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">unless the driver supports the associated data type</span>
<span style="font-family:'ZapfDingbats';font-size:4.92pt;color:#000000">I</span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">createNClob </span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">unless the driver supports the associated data type</span>
<span style="font-family:'ZapfDingbats';font-size:4.92pt;color:#000000">I</span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">createSQLXML </span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">unless the driver supports the associated data type</span>
<span style="font-family:'ZapfDingbats';font-size:4.92pt;color:#000000">I</span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">createStruct </span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">unless the driver supports the associated data type</span>

<span style="font-family:'Helvetica';font-size:7.91pt;color:#000000">Chapter 6</span>
<span style="font-family:'Helvetica';font-size:7.91pt;color:#000000">Compliance</span>
<span style="font-family:'Helvetica-Bold';font-size:7.91pt;font-weight:bold;color:#000000">31</span>

---

## Page 47

<span style="font-family:'ZapfDingbats';font-size:4.92pt;color:#000000">I</span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">getTypeMap </span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">unless the driver supports the associated data type</span>
<span style="font-family:'ZapfDingbats';font-size:4.92pt;color:#000000">I</span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">setTypeMap </span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">unless the driver supports the associated data type</span>
<span style="font-family:'ZapfDingbats';font-size:4.92pt;color:#000000">I</span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">prepareStatement(String sql, </span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">Statement.RETURN_GENERATED_KEYS)</span>
<span style="font-family:'ZapfDingbats';font-size:4.92pt;color:#000000">I</span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">prepareStatement(String sql, int[] columnIndexes)</span>
<span style="font-family:'ZapfDingbats';font-size:4.92pt;color:#000000">I</span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">prepareStatement(String sql, String[] columnNames)</span>
<span style="font-family:'ZapfDingbats';font-size:4.92pt;color:#000000">I</span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">setSavePoint</span>
<span style="font-family:'ZapfDingbats';font-size:4.92pt;color:#000000">I</span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">rollback(java.sql.SavePoint savepoint)</span>
<span style="font-family:'ZapfDingbats';font-size:4.92pt;color:#000000">I</span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">releaseSavePoint</span>
<span style="font-family:'ZapfDingbats';font-size:4.92pt;color:#000000">I</span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">setNetworkTimeout</span>
<span style="font-family:'ZapfDingbats';font-size:5.99pt;color:#000000">I</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> It must implement the </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">Statement</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> interface with the exception of the following </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">optional methods:</span>
<span style="font-family:'ZapfDingbats';font-size:4.92pt;color:#000000">I</span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">cancel</span>
<span style="font-family:'ZapfDingbats';font-size:4.92pt;color:#000000">I</span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">execute(String sql, Statement.RETURN_GENERATED_KEYS)</span>
<span style="font-family:'ZapfDingbats';font-size:4.92pt;color:#000000">I</span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">execute(String sql, int[] columnIndexes)</span>
<span style="font-family:'ZapfDingbats';font-size:4.92pt;color:#000000">I</span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">execute(String sql, String[] columnNames)</span>
<span style="font-family:'ZapfDingbats';font-size:4.92pt;color:#000000">I</span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">executeUpdate(String sql, Statement.RETURN_GENERATED_KEYS)</span>
<span style="font-family:'ZapfDingbats';font-size:4.92pt;color:#000000">I</span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">executeUpdate(String sql, int[] columnIndexes)</span>
<span style="font-family:'ZapfDingbats';font-size:4.92pt;color:#000000">I</span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">executeUpdate(String sql, String[] columnNames)</span>
<span style="font-family:'ZapfDingbats';font-size:4.92pt;color:#000000">I</span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">getGeneratedKeys</span>
<span style="font-family:'ZapfDingbats';font-size:4.92pt;color:#000000">I</span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">getMoreResults(Statement.KEEP_CURRENT_RESULT) </span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">unless </span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">DatabasemetaData.supportsMultipleOpenResults()</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> returns </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">true</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">.</span>
<span style="font-family:'ZapfDingbats';font-size:4.92pt;color:#000000">I</span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">getMoreResults(Statement.CLOSE_ALL_RESULTS) </span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">unless </span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">DatabasemetaData.supportsMultipleOpenResults()</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> returns </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">true</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">.</span>
<span style="font-family:'ZapfDingbats';font-size:4.92pt;color:#000000">I</span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">setCursorName</span>
<span style="font-family:'ZapfDingbats';font-size:5.99pt;color:#000000">I</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> It must implement the </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">PreparedStatement</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> interface with the exception of the </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">following optional methods:</span>
<span style="font-family:'ZapfDingbats';font-size:4.92pt;color:#000000">I</span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">getMetaData</span>
<span style="font-family:'ZapfDingbats';font-size:4.92pt;color:#000000">I</span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">setArray</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">, </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">setBlob</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">, </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">setClob</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">, </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">setNClob</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">, </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">setNCharacterStream</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">, </span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">setNString</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">, </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">setRef</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">, </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">setRowId</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">, </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">setSQLXML</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> and </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">setURL</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> unless the driver </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">supports the associated data type</span>
<span style="font-family:'ZapfDingbats';font-size:4.92pt;color:#000000">I</span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">setNull(int parameterIndex,int sqlType, String typeName) </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">unless the driver supports the associated data type</span>
<span style="font-family:'ZapfDingbats';font-size:4.92pt;color:#000000">I</span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">setUnicodeStream</span>
<span style="font-family:'ZapfDingbats';font-size:4.92pt;color:#000000">I</span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">setAsciiStream, setBinaryStream, setCharacterStream, </span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">setNCharacterStream </span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">which do not take a length parameter</span>

<span style="font-family:'Helvetica-Bold';font-size:7.91pt;font-weight:bold;color:#000000">32</span>
<span style="font-family:'Helvetica';font-size:7.91pt;color:#000000">JDBC 4.2 Specification • March 2014</span>

---

## Page 48

<span style="font-family:'ZapfDingbats';font-size:4.92pt;color:#000000">I</span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">getObject(int i, Class&lt;T&gt; type)</span>

<span style="font-family:'ZapfDingbats';font-size:4.92pt;color:#000000">I</span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">All </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">updateXXX</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> methods </span>
<span style="font-family:'ZapfDingbats';font-size:4.92pt;color:#000000">I</span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">absolute</span>
<span style="font-family:'ZapfDingbats';font-size:4.92pt;color:#000000">I</span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">afterLast</span>
<span style="font-family:'ZapfDingbats';font-size:4.92pt;color:#000000">I</span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">beforeFirst</span>
<span style="font-family:'ZapfDingbats';font-size:4.92pt;color:#000000">I</span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">cancelRowUpdates</span>
<span style="font-family:'ZapfDingbats';font-size:4.92pt;color:#000000">I</span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">deleteRow</span>
<span style="font-family:'ZapfDingbats';font-size:4.92pt;color:#000000">I</span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">first</span>

<span style="font-family:'ZapfDingbats';font-size:4.92pt;color:#000000">I</span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">getBigDecimal(int i,int scale)</span>
<span style="font-family:'ZapfDingbats';font-size:4.92pt;color:#000000">I</span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">getBigDecimal(String colName,int scale)</span>
<span style="font-family:'ZapfDingbats';font-size:4.92pt;color:#000000">I</span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">getCursorName</span>
<span style="font-family:'ZapfDingbats';font-size:4.92pt;color:#000000">I</span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">getObject(int i, Class&lt;T&gt; type)</span>
<span style="font-family:'ZapfDingbats';font-size:5.99pt;color:#000000">I</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> It must implement the </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">CallableStatement</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> interface if the method </span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">DatabaseMetaData</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">.</span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">supportsStoredProcedures()</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> returns </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">true</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> with the </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">exception of the following optional methods:</span>
<span style="font-family:'ZapfDingbats';font-size:4.92pt;color:#000000">I</span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">All </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">setXXX</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">, </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">getXXX</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> and </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">registerOutputParameter</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> methods which </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">support named parameters</span>
<span style="font-family:'ZapfDingbats';font-size:4.92pt;color:#000000">I</span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">getArray</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">, </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">getBlob</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">, </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">getClob</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">, </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">getNClob</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">, </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">getNCharacterStream</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">, </span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">getNString</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">, </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">getRef</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">, </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">getRowId</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">, </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">getSQLXML</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> and </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">getURL</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> unless the driver </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">supports the associated data type</span>
<span style="font-family:'ZapfDingbats';font-size:4.92pt;color:#000000">I</span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">getBigDecimal(int parameterIndex,int scale)</span>
<span style="font-family:'ZapfDingbats';font-size:4.92pt;color:#000000">I</span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">getObject(String colName, Class&lt;T&gt; type)</span>
<span style="font-family:'ZapfDingbats';font-size:4.92pt;color:#000000">I</span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">getObject(int parameterIndex, </span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">java.util.Map&lt;java.lang.String,java.lang.Class&lt;?&gt;&gt; map) </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">unless the driver supports the associated data type</span>
<span style="font-family:'ZapfDingbats';font-size:4.92pt;color:#000000">I</span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">registerOutputParam(String parameterName,int sqlType, </span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">String typeName) u</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">nless the driver supports the associated data type</span>
<span style="font-family:'ZapfDingbats';font-size:4.92pt;color:#000000">I</span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">setNull(String parameterName,int sqlType, String typeName) </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">unless the driver supports the associated data type</span>
<span style="font-family:'ZapfDingbats';font-size:4.92pt;color:#000000">I</span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">setAsciiStream, setBinaryStream, setCharacterStream, </span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">setNCharacterStream </span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">which do not take a length parameter</span>
<span style="font-family:'ZapfDingbats';font-size:5.99pt;color:#000000">I</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> It must implement the </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">ResultSet</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> interface with the exception of the following </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">optional methods:</span>
<span style="font-family:'ZapfDingbats';font-size:4.92pt;color:#000000">I</span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">getArray</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">, </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">getBlob</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">, </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">getClob</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">, </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">getNClob</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">, </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">getNCharacterStream</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">, </span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">getNString</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">, </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">getRef</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">, </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">getRowId</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">, </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">getSQLXML</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> and </span><span style="font-family:'Courier';font-size:9.47pt;color:#000000">getURL</span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000"> unless the driver </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">supports the associated data type</span>
<span style="font-family:'ZapfDingbats';font-size:4.92pt;color:#000000">I</span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">getObject(String colName, Class&lt;T&gt; type)</span>
<span style="font-family:'ZapfDingbats';font-size:4.92pt;color:#000000">I</span>
<span style="font-family:'Courier';font-size:9.47pt;color:#000000">getObject(int i, Map&lt;String,Class&lt;?&gt;&gt; map) </span><span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">unless the driver </span>
<span style="font-family:'Palatino-Roman';font-size:9.47pt;color:#000000">supports the associated data type</span>

<span style="font-family:'Helvetica';font-size:7.91pt;color:#000000">Chapter 6</span>
<span style="font-family:'Helvetica';font-size:7.91pt;color:#000000">Compliance</span>
<span style="font-family:'Helvetica-Bold';font-size:7.91pt;font-weight:bold;color:#000000"