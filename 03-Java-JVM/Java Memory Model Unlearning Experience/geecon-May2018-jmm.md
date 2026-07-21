## Page 1

<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#ffffff">Java Memory Model Unlearning Experience</span>
<span style="font-family:'DroidSans-Bold';font-size:11.96pt;font-weight:bold;color:#ffffff">or, «Crazy Russian Guy Yells About JMM»</span>
<span style="font-family:'DroidSans-Bold';font-size:11.96pt;font-weight:bold;color:#ffffff">Aleksey Shipilёv</span>
<span style="font-family:'DroidSans-Bold';font-size:11.96pt;font-weight:bold;color:#ffffff">shade@redhat.com</span>
<span style="font-family:'DroidSans-Bold';font-size:11.96pt;font-weight:bold;color:#ffffff">@shipilev</span>

![](./geecon-May2018-jmm_assets/images/p001_img01.png)

---

## Page 2

<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">Safe Harbor / Тихая Гавань</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Anything on this or any subsequent slides may be a lie. Do</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">not base your decisions on this talk. If you do, ask for</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">professional help.</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Всё что угодно на этом слайде, как и на всех следующих,</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">может быть враньём. Не принимайте решений на</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">основании этого доклада. Если всё-таки решите принять,</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">то наймите профессионалов.</span>

<span style="font-family:'DroidSans';font-size:5.98pt;color:#d9d9d9">Slide 2/86. «Java Memory Model Unlearning Experience», Aleksey Shipilёv, 2018, D:20180511133537+02’00’</span>

---

## Page 3

Theory

<!-- 图源: ./geecon-May2018-jmm_assets/images/p003_scan.jpg -->


<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">Theory</span>

---

## Page 4

<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">Spec: ...vs Implementation</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Everybody intuitively understands the difference between</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">the</span><span style="font-family:'DroidSans-Slant_213';font-size:14.35pt;font-style:italic;color:#000000"> speciﬁcation</span><span style="font-family:'DroidSans';font-size:14.35pt;color:#000000"> and the</span><span style="font-family:'DroidSans-Slant_213';font-size:14.35pt;font-style:italic;color:#000000"> implementation</span>
<span style="font-family:'F44';font-size:11.96pt;color:#008000">class</span><span style="font-family:'F44';font-size:11.96pt;color:#0000ff"> Integer</span><span style="font-family:'F44';font-size:11.96pt;color:#666666"> {</span>
<span style="font-family:'F45';font-size:11.96pt;color:#408080">/**</span>
<span style="font-family:'F45';font-size:11.96pt;color:#408080">* Returns a {@code String} object representing the</span>
<span style="font-family:'F45';font-size:11.96pt;color:#408080">* specified integer. The argument is converted to signed decimal</span>
<span style="font-family:'F45';font-size:11.96pt;color:#408080">* representation and returned as a string, exactly as if ...</span>
<span style="font-family:'F45';font-size:11.96pt;color:#408080">*/</span>
<span style="font-family:'F44';font-size:11.96pt;color:#008000">public static</span><span style="font-family:'F44';font-size:11.96pt;color:#000000"> String</span><span style="font-family:'F44';font-size:11.96pt;color:#0000ff"> toString</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">(</span><span style="font-family:'F44';font-size:11.96pt;color:#b00040">int</span><span style="font-family:'F44';font-size:11.96pt;color:#000000"> i</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">) {</span>
<span style="font-family:'F45';font-size:11.96pt;color:#408080">// Who cares what is going on here?</span>
<span style="font-family:'F44';font-size:11.96pt;color:#666666">}</span>
<span style="font-family:'F44';font-size:11.96pt;color:#666666">}</span>
<span style="font-family:'DroidSans';font-size:5.98pt;color:#d9d9d9">Slide 4/86. «Java Memory Model Unlearning Experience», Aleksey Shipilёv, 2018, D:20180511133537+02’00’</span>

---

## Page 5

<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">Spec: Good Spec Is A Balance</span>
<span style="font-family:'DroidSans-Bold';font-size:14.35pt;font-weight:bold;color:#000000">Under</span><span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">specify, and things become unusable:</span>

<span style="font-family:'F45';font-size:11.96pt;color:#408080">/**</span>
<span style="font-family:'F45';font-size:11.96pt;color:#408080">* This method can do whatever it pleases.</span>
<span style="font-family:'F45';font-size:11.96pt;color:#408080">*/</span>
<span style="font-family:'F44';font-size:11.96pt;color:#008000">public</span><span style="font-family:'F44';font-size:11.96pt;color:#b00040"> void</span><span style="font-family:'F44';font-size:11.96pt;color:#0000ff"> summonNasalDemons</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">(</span><span style="font-family:'F44';font-size:11.96pt;color:#b00040">int</span><span style="font-family:'F44';font-size:11.96pt;color:#000000"> count</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">) { ... }</span>
<span style="font-family:'DroidSans-Bold';font-size:14.35pt;font-weight:bold;color:#000000">Over</span><span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">specify, and implementation choices are limited:</span>
<span style="font-family:'F45';font-size:11.96pt;color:#408080">/**</span>
<span style="font-family:'F45';font-size:11.96pt;color:#408080">* This method checks if Java program halts.</span>
<span style="font-family:'F45';font-size:11.96pt;color:#408080">*/</span>
<span style="font-family:'F44';font-size:11.96pt;color:#008000">public</span><span style="font-family:'F44';font-size:11.96pt;color:#b00040"> boolean</span><span style="font-family:'F44';font-size:11.96pt;color:#0000ff"> checkHalt</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">(</span><span style="font-family:'F44';font-size:11.96pt;color:#000000">String program</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">) { ... }</span>
<span style="font-family:'DroidSans';font-size:5.98pt;color:#d9d9d9">Slide 5/86. «Java Memory Model Unlearning Experience», Aleksey Shipilёv, 2018, D:20180511133537+02’00’</span>

---

## Page 6

<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">Spec: Abstract Machines</span>

<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Language semantics is</span><span style="font-family:'DroidSans-Slant_213';font-size:14.35pt;font-style:italic;color:#000000"> speciﬁed</span><span style="font-family:'DroidSans';font-size:14.35pt;color:#000000"> by the behavior</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">of the</span><span style="font-family:'DroidSans-Slant_213';font-size:14.35pt;font-style:italic;color:#000000"> abstract machine</span>
<span style="font-family:'F44';font-size:11.96pt;color:#008000">public</span><span style="font-family:'F44';font-size:11.96pt;color:#b00040"> int</span><span style="font-family:'F44';font-size:11.96pt;color:#0000ff"> m</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">() {</span>
<span style="font-family:'F44';font-size:11.96pt;color:#b00040">int</span><span style="font-family:'F44';font-size:11.96pt;color:#000000"> x</span><span style="font-family:'F44';font-size:11.96pt;color:#666666"> = 42;</span>
<span style="font-family:'F44';font-size:11.96pt;color:#b00040">int</span><span style="font-family:'F44';font-size:11.96pt;color:#000000"> y</span><span style="font-family:'F44';font-size:11.96pt;color:#666666"> = 34;</span>
<span style="font-family:'F44';font-size:11.96pt;color:#b00040">int</span><span style="font-family:'F44';font-size:11.96pt;color:#000000"> t</span><span style="font-family:'F44';font-size:11.96pt;color:#666666"> =</span><span style="font-family:'F44';font-size:11.96pt;color:#000000"> x</span><span style="font-family:'F44';font-size:11.96pt;color:#666666"> +</span><span style="font-family:'F44';font-size:11.96pt;color:#000000"> y</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">;</span>
<span style="font-family:'F44';font-size:11.96pt;color:#008000">return</span><span style="font-family:'F44';font-size:11.96pt;color:#000000"> t</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">;</span>
<span style="font-family:'F44';font-size:11.96pt;color:#666666">}</span>

<span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000">⇒</span>

<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">If the result is not distinguishable from the</span><span style="font-family:'DroidSans-Slant_213';font-size:14.35pt;font-style:italic;color:#000000"> abstract machine</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">behavior, nobody cares how it was achieved!</span>
<span style="font-family:'DroidSans';font-size:5.98pt;color:#d9d9d9">Slide 6/86. «Java Memory Model Unlearning Experience», Aleksey Shipilёv, 2018, D:20180511133537+02’00’</span>

<span style="font-family:'F44';font-size:11.96pt;color:#a1a100">m:</span>
<span style="font-family:'F44';font-size:11.96pt;color:#7d8f29">...prolog...</span>
<span style="font-family:'F44';font-size:11.96pt;color:#0000ff">mov</span><span style="font-family:'F44';font-size:11.96pt;color:#870000"> $76$</span><span style="font-family:'F44';font-size:11.96pt;color:#000000">,</span><span style="font-family:'F44';font-size:11.96pt;color:#1a177d"> %rax</span>
<span style="font-family:'F44';font-size:11.96pt;color:#7d8f29">...epilog...</span>
<span style="font-family:'F44';font-size:11.96pt;color:#0000ff">ret</span>

---

## Page 7

<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">Spec: JMM Is Part Of Abstract Machine</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">If the result is not distinguishable from the</span><span style="font-family:'DroidSans-Slant_213';font-size:14.35pt;font-style:italic;color:#000000"> abstract machine</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">behavior, nobody cares how it was achieved!</span>

<span style="font-family:'F44';font-size:11.96pt;color:#008000">volatile</span><span style="font-family:'F44';font-size:11.96pt;color:#b00040"> int</span><span style="font-family:'F44';font-size:11.96pt;color:#000000"> x</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">;</span>
<span style="font-family:'F44';font-size:11.96pt;color:#008000">public</span><span style="font-family:'F44';font-size:11.96pt;color:#b00040"> int</span><span style="font-family:'F44';font-size:11.96pt;color:#0000ff"> m</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">() {</span>
<span style="font-family:'F44';font-size:11.96pt;color:#000000">x</span><span style="font-family:'F44';font-size:11.96pt;color:#666666"> = 1;</span>
<span style="font-family:'F44';font-size:11.96pt;color:#000000">x</span><span style="font-family:'F44';font-size:11.96pt;color:#666666"> = 2;</span>
<span style="font-family:'F44';font-size:11.96pt;color:#008000">return</span><span style="font-family:'F44';font-size:11.96pt;color:#000000"> x</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">;</span>
<span style="font-family:'F44';font-size:11.96pt;color:#666666">}</span>

<span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000">⇒</span>

<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">(In practice, not all optimizations are... practical)</span>

<span style="font-family:'DroidSans';font-size:5.98pt;color:#d9d9d9">Slide 7/86. «Java Memory Model Unlearning Experience», Aleksey Shipilёv, 2018, D:20180511133537+02’00’</span>

<span style="font-family:'F44';font-size:11.96pt;color:#a1a100">m:</span>
<span style="font-family:'F44';font-size:11.96pt;color:#7d8f29">...prolog...</span>
<span style="font-family:'F44';font-size:11.96pt;color:#0000ff">mov</span><span style="font-family:'F44';font-size:11.96pt;color:#870000"> $2$</span><span style="font-family:'F44';font-size:11.96pt;color:#000000">, (</span><span style="font-family:'F44';font-size:11.96pt;color:#870000">mem</span><span style="font-family:'F44';font-size:11.96pt;color:#000000">)</span>
<span style="font-family:'F44';font-size:11.96pt;color:#0000ff">mov</span><span style="font-family:'F44';font-size:11.96pt;color:#870000"> $2$</span><span style="font-family:'F44';font-size:11.96pt;color:#000000">,</span><span style="font-family:'F44';font-size:11.96pt;color:#1a177d"> %rax</span>
<span style="font-family:'F44';font-size:11.96pt;color:#7d8f29">...epilog...</span>
<span style="font-family:'F44';font-size:11.96pt;color:#0000ff">ret</span>

---

## Page 8

<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">JMM: Talk Idea</span>

<span style="font-family:'DroidSans';font-size:17.22pt;color:#000000">JMM is simple! (Not joking.)</span>

<span style="font-family:'DroidSans';font-size:5.98pt;color:#d9d9d9">Slide 8/86. «Java Memory Model Unlearning Experience», Aleksey Shipilёv, 2018, D:20180511133537+02’00’</span>

---

## Page 9

<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">JMM: Talk Idea</span>

<span style="font-family:'DroidSans';font-size:17.22pt;color:#000000">JMM is simple! (Not joking.)</span>

<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">The problem is educational:</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Most JMM talks discuss what JMM</span><span style="font-family:'DroidSans-Bold';font-size:14.35pt;font-weight:bold;color:#000000"> is</span><span style="font-family:'DroidSans';font-size:14.35pt;color:#000000"> about:</span>

<span style="font-family:'DroidSans';font-size:11.96pt;color:#000000">Works, but piles on naive misconceptions</span>
<span style="font-family:'DroidSans';font-size:11.96pt;color:#000000">Also talks about implementations, blurring the whole thing</span>

<span style="font-family:'DroidSans';font-size:5.98pt;color:#d9d9d9">Slide 8/86. «Java Memory Model Unlearning Experience», Aleksey Shipilёv, 2018, D:20180511133537+02’00’</span>

---

## Page 10

<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">JMM: Talk Idea</span>

<span style="font-family:'DroidSans';font-size:17.22pt;color:#000000">JMM is simple! (Not joking.)</span>

<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">The problem is educational:</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Most JMM talks discuss what JMM</span><span style="font-family:'DroidSans-Bold';font-size:14.35pt;font-weight:bold;color:#000000"> is</span><span style="font-family:'DroidSans';font-size:14.35pt;color:#000000"> about:</span>
<span style="font-family:'DroidSans';font-size:11.96pt;color:#000000">Works, but piles on naive misconceptions</span>
<span style="font-family:'DroidSans';font-size:11.96pt;color:#000000">Also talks about implementations, blurring the whole thing</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">This talk discusses what JMM</span><span style="font-family:'DroidSans-Bold';font-size:14.35pt;font-weight:bold;color:#000000"> is not</span><span style="font-family:'DroidSans';font-size:14.35pt;color:#000000"> about:</span>

<span style="font-family:'DroidSans';font-size:11.96pt;color:#000000">This is the</span><span style="font-family:'DroidSans-Bold';font-size:11.96pt;font-weight:bold;color:#000000"> unlearning</span><span style="font-family:'DroidSans';font-size:11.96pt;color:#000000"> experience!</span>
<span style="font-family:'DroidSans';font-size:11.96pt;color:#000000">(And we try to deconstruct misconceptions)</span>

<span style="font-family:'DroidSans';font-size:5.98pt;color:#d9d9d9">Slide 8/86. «Java Memory Model Unlearning Experience», Aleksey Shipilёv, 2018, D:20180511133537+02’00’</span>

---

## Page 11

<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">JMM: Problem</span>

<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">«Oh, give me 5 minutes to read up on JMM!»</span>

<span style="font-family:'DroidSans';font-size:5.98pt;color:#d9d9d9">Slide 9/86. «Java Memory Model Unlearning Experience», Aleksey Shipilёv, 2018, D:20180511133537+02’00’</span>

![](./geecon-May2018-jmm_assets/images/p011_img01.png)



![](./geecon-May2018-jmm_assets/images/p011_img02.jpg)

---

## Page 12

<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">JMM: Actions and Executions</span>

<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Executions</span><span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000"> ≈</span><span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Actions</span><span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000"> ∪</span><span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Orders</span><span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000"> ∪</span><span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Consistency Rules</span>

<span style="font-family:'DroidSans';font-size:5.98pt;color:#d9d9d9">Slide 10/86. «Java Memory Model Unlearning Experience», Aleksey Shipilёv, 2018, D:20180511133537+02’00’</span>

---

## Page 13

<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">JMM: Actions and Executions</span>

<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Executions</span><span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000"> ≈</span><span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Actions</span><span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000"> ∪</span><span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Orders</span><span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000"> ∪</span><span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Consistency Rules</span>

<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Executions are the behaviors of the</span><span style="font-family:'DroidSans-Bold';font-size:14.35pt;font-weight:bold;color:#000000"> abstract machine</span><span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">, not</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">the behavior of ﬁnal implementation. They deﬁne all possible</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">ways the Java program can possibly execute.</span>

<span style="font-family:'DroidSans';font-size:5.98pt;color:#d9d9d9">Slide 10/86. «Java Memory Model Unlearning Experience», Aleksey Shipilёv, 2018, D:20180511133537+02’00’</span>

---

## Page 14

<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">JMM: Actions and Executions</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Executions</span><span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000"> ≈</span><span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Actions</span><span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000"> ∪</span><span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Orders</span><span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000"> ∪</span><span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Consistency Rules</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Actions:</span>
<span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑤</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑓𝑖𝑒𝑙𝑑, 𝑉</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">)</span><span style="font-family:'DroidSans';font-size:14.35pt;color:#000000"> – write value</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000"> 𝑉</span><span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">into</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000"> 𝑓𝑖𝑒𝑙𝑑</span>
<span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑟</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑓𝑖𝑒𝑙𝑑</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">) :</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000"> 𝑉</span><span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">– read value</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000"> 𝑉</span><span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">from</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000"> 𝑓𝑖𝑒𝑙𝑑</span>
<span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝐿</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑚𝑜𝑛𝑖𝑡𝑜𝑟</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">)</span><span style="font-family:'DroidSans';font-size:14.35pt;color:#000000"> – lock the</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000"> 𝑚𝑜𝑛𝑖𝑡𝑜𝑟</span>
<span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑈𝐿</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑚𝑜𝑛𝑖𝑡𝑜𝑟</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">)</span><span style="font-family:'DroidSans';font-size:14.35pt;color:#000000"> – unlock the</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000"> 𝑚𝑜𝑛𝑖𝑡𝑜𝑟</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">...</span>
<span style="font-family:'DroidSans';font-size:5.98pt;color:#d9d9d9">Slide 10/86. «Java Memory Model Unlearning Experience», Aleksey Shipilёv, 2018, D:20180511133537+02’00’</span>

---

## Page 15

<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">JMM: Actions and Executions</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Executions</span><span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000"> ≈</span><span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Actions</span><span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000"> ∪</span><span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Orders</span><span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000"> ∪</span><span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Consistency Rules</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Orders:</span>

<span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑤</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑎,</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000"> 1)</span>

<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Consistency rules:</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">PO consistency</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">SO consistency, SO - PO consistency</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">HB consistency</span>
<span style="font-family:'DroidSans';font-size:5.98pt;color:#d9d9d9">Slide 10/86. «Java Memory Model Unlearning Experience», Aleksey Shipilёv, 2018, D:20180511133537+02’00’</span>

<span style="font-family:'DroidSans';font-size:7.97pt;color:#000000">hb</span>
<span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000">−−→</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑟</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑎</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">) : 1</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000"> ... 𝑤</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑎,</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000"> 2)</span>

---

## Page 16

<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">JMM: Umm...</span>

<span style="font-family:'DroidSans';font-size:5.98pt;color:#d9d9d9">Slide 11/86. «Java Memory Model Unlearning Experience», Aleksey Shipilёv, 2018, D:20180511133537+02’00’</span>

![](./geecon-May2018-jmm_assets/images/p016_img01.jpg)

---

## Page 17

<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">JMM: Why?</span>

<span style="font-family:'DroidSans';font-size:7.97pt;color:#ff0000">JMM</span>

<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Original</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Program</span>

<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Some</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Implementation</span>

<span style="font-family:'DroidSans';font-size:7.97pt;color:#0000ff">Impl</span>

<span style="font-family:'DroidSans';font-size:5.98pt;color:#d9d9d9">Slide 12/86. «Java Memory Model Unlearning Experience», Aleksey Shipilёv, 2018, D:20180511133537+02’00’</span>

<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">{Executions}</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">{Outcomes}</span>

<span style="font-family:'DroidSans';font-size:7.97pt;color:#ff0000">yield</span>

<span style="font-family:'DroidSans';font-size:7.97pt;color:#000000">subset of</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Some</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Results</span>

<span style="font-family:'DroidSans';font-size:7.97pt;color:#0000ff">yields</span>

---

## Page 18

<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">JMM: Why?</span>

<span style="font-family:'DroidSans';font-size:7.97pt;color:#ff0000">JMM</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">{Executions}</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">{Outcomes}</span>
<span style="font-family:'DroidSans';font-size:7.97pt;color:#ff0000">yield</span>

<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Original</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Program</span>
<span style="font-family:'DroidSans';font-size:7.97pt;color:#000000">subset of</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Some</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Results</span>

<span style="font-family:'DroidSans';font-size:7.97pt;color:#0000ff">Impl</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Some</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Implementation</span>
<span style="font-family:'DroidSans';font-size:7.97pt;color:#0000ff">yields</span>

<span style="font-family:'F19';font-size:14.35pt;color:#000000">int</span><span style="font-family:'F19';font-size:14.35pt;color:#ff0000"> a</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> = 0</span>
<span style="font-family:'F19';font-size:14.35pt;color:#ff0000">a</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> = 1</span>
<span style="font-family:'F19';font-size:14.35pt;color:#ff0000">a</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> = 2</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">r1 =</span><span style="font-family:'F19';font-size:14.35pt;color:#ff0000"> a</span>

<span style="font-family:'DroidSans';font-size:5.98pt;color:#d9d9d9">Slide 12/86. «Java Memory Model Unlearning Experience», Aleksey Shipilёv, 2018, D:20180511133537+02’00’</span>

---

## Page 19

<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">JMM: Why?</span>

<span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑤</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑎,</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000"> 1)</span>
<span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑤</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑎,</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000"> 1)</span>
<span style="font-family:'DroidSans';font-size:7.97pt;color:#ff0000">JMM</span>
<span style="font-family:'DroidSans';font-size:7.97pt;color:#000000">hb</span>
<span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000">−−→</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑟</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑎</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">) : 1</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000"> ... 𝑤</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑎,</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000"> 2)</span>
<span style="font-family:'DroidSans';font-size:7.97pt;color:#000000">hb</span>
<span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000">−−→</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑟</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑎</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">) : 2</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000"> ... 𝑤</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑎,</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000"> 2)</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">{Executions}</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">{Outcomes}</span>

<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Original</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Program</span>
<span style="font-family:'DroidSans';font-size:7.97pt;color:#ff0000">yield</span>

<span style="font-family:'DroidSans';font-size:7.97pt;color:#000000">subset of</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Some</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Results</span>

<span style="font-family:'DroidSans';font-size:7.97pt;color:#0000ff">Impl</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Some</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Implementation</span>
<span style="font-family:'DroidSans';font-size:7.97pt;color:#0000ff">yields</span>

<span style="font-family:'F19';font-size:14.35pt;color:#000000">int</span><span style="font-family:'F19';font-size:14.35pt;color:#ff0000"> a</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> = 0</span>
<span style="font-family:'F19';font-size:14.35pt;color:#ff0000">a</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> = 1</span>
<span style="font-family:'F19';font-size:14.35pt;color:#ff0000">a</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> = 2</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">r1 =</span><span style="font-family:'F19';font-size:14.35pt;color:#ff0000"> a</span>

<span style="font-family:'DroidSans';font-size:5.98pt;color:#d9d9d9">Slide 12/86. «Java Memory Model Unlearning Experience», Aleksey Shipilёv, 2018, D:20180511133537+02’00’</span>

---

## Page 20

<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">JMM: Why?</span>

<span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑤</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑎,</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000"> 1)</span>
<span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑤</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑎,</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000"> 1)</span>
<span style="font-family:'DroidSans';font-size:7.97pt;color:#ff0000">JMM</span>
<span style="font-family:'DroidSans';font-size:7.97pt;color:#000000">hb</span>
<span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000">−−→</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑟</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑎</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">) : 1</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000"> ... 𝑤</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑎,</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000"> 2)</span>
<span style="font-family:'DroidSans';font-size:7.97pt;color:#000000">hb</span>
<span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000">−−→</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑟</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑎</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">) : 2</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000"> ... 𝑤</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑎,</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000"> 2)</span>
<span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑟</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">1</span><span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000"> ∈{</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">1</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">,</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000"> 2</span><span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000">}</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">{Executions}</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">{Outcomes}</span>

<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Original</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Program</span>
<span style="font-family:'DroidSans';font-size:7.97pt;color:#ff0000">yield</span>

<span style="font-family:'DroidSans';font-size:7.97pt;color:#000000">subset of</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Some</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Results</span>

<span style="font-family:'DroidSans';font-size:7.97pt;color:#0000ff">Impl</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Some</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Implementation</span>
<span style="font-family:'DroidSans';font-size:7.97pt;color:#0000ff">yields</span>

<span style="font-family:'F19';font-size:14.35pt;color:#000000">int</span><span style="font-family:'F19';font-size:14.35pt;color:#ff0000"> a</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> = 0</span>
<span style="font-family:'F19';font-size:14.35pt;color:#ff0000">a</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> = 1</span>
<span style="font-family:'F19';font-size:14.35pt;color:#ff0000">a</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> = 2</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">r1 =</span><span style="font-family:'F19';font-size:14.35pt;color:#ff0000"> a</span>

<span style="font-family:'DroidSans';font-size:5.98pt;color:#d9d9d9">Slide 12/86. «Java Memory Model Unlearning Experience», Aleksey Shipilёv, 2018, D:20180511133537+02’00’</span>

---

## Page 21

<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">JMM: Why?</span>

<span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑤</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑎,</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000"> 1)</span>
<span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑤</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑎,</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000"> 1)</span>
<span style="font-family:'DroidSans';font-size:7.97pt;color:#ff0000">JMM</span>
<span style="font-family:'DroidSans';font-size:7.97pt;color:#000000">hb</span>
<span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000">−−→</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑟</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑎</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">) : 1</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000"> ... 𝑤</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑎,</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000"> 2)</span>
<span style="font-family:'DroidSans';font-size:7.97pt;color:#000000">hb</span>
<span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000">−−→</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑟</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑎</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">) : 2</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000"> ... 𝑤</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑎,</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000"> 2)</span>
<span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑟</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">1</span><span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000"> ∈{</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">1</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">,</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000"> 2</span><span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000">}</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">{Executions}</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">{Outcomes}</span>

<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Original</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Program</span>
<span style="font-family:'DroidSans';font-size:7.97pt;color:#ff0000">yield</span>

<span style="font-family:'DroidSans';font-size:7.97pt;color:#000000">subset of</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Some</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Results</span>

<span style="font-family:'DroidSans';font-size:7.97pt;color:#0000ff">Impl</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Some</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Implementation</span>
<span style="font-family:'DroidSans';font-size:7.97pt;color:#0000ff">yields</span>

<span style="font-family:'F19';font-size:14.35pt;color:#000000">int</span><span style="font-family:'F19';font-size:14.35pt;color:#ff0000"> a</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> = 0</span>
<span style="font-family:'F19';font-size:14.35pt;color:#ff0000">a</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> = 1</span>
<span style="font-family:'F19';font-size:14.35pt;color:#ff0000">a</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> = 2</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">r1 =</span><span style="font-family:'F19';font-size:14.35pt;color:#ff0000"> a</span>

<span style="font-family:'F19';font-size:14.35pt;color:#000000">mov 1</span><span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000"> →</span><span style="font-family:'F19';font-size:14.35pt;color:#000000">(a)</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">mov 1</span><span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000"> →</span><span style="font-family:'F19';font-size:14.35pt;color:#000000">(r1)</span>

<span style="font-family:'DroidSans';font-size:5.98pt;color:#d9d9d9">Slide 12/86. «Java Memory Model Unlearning Experience», Aleksey Shipilёv, 2018, D:20180511133537+02’00’</span>

---

## Page 22

<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">JMM: Why?</span>

<span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑤</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑎,</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000"> 1)</span>
<span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑤</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑎,</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000"> 1)</span>
<span style="font-family:'DroidSans';font-size:7.97pt;color:#ff0000">JMM</span>
<span style="font-family:'DroidSans';font-size:7.97pt;color:#000000">hb</span>
<span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000">−−→</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑟</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑎</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">) : 1</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000"> ... 𝑤</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑎,</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000"> 2)</span>
<span style="font-family:'DroidSans';font-size:7.97pt;color:#000000">hb</span>
<span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000">−−→</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑟</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑎</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">) : 2</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000"> ... 𝑤</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑎,</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000"> 2)</span>
<span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑟</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">1</span><span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000"> ∈{</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">1</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">,</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000"> 2</span><span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000">}</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">{Executions}</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">{Outcomes}</span>

<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Original</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Program</span>
<span style="font-family:'DroidSans';font-size:7.97pt;color:#ff0000">yield</span>

<span style="font-family:'DroidSans';font-size:7.97pt;color:#000000">subset of</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Some</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Results</span>

<span style="font-family:'DroidSans';font-size:7.97pt;color:#0000ff">Impl</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Some</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Implementation</span>
<span style="font-family:'DroidSans';font-size:7.97pt;color:#0000ff">yields</span>

<span style="font-family:'F19';font-size:14.35pt;color:#000000">int</span><span style="font-family:'F19';font-size:14.35pt;color:#ff0000"> a</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> = 0</span>
<span style="font-family:'F19';font-size:14.35pt;color:#ff0000">a</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> = 1</span>
<span style="font-family:'F19';font-size:14.35pt;color:#ff0000">a</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> = 2</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">r1 =</span><span style="font-family:'F19';font-size:14.35pt;color:#ff0000"> a</span>

<span style="font-family:'F19';font-size:14.35pt;color:#000000">mov 1</span><span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000"> →</span><span style="font-family:'F19';font-size:14.35pt;color:#000000">(a)</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">mov 1</span><span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000"> →</span><span style="font-family:'F19';font-size:14.35pt;color:#000000">(r1)</span>
<span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑟</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">1</span><span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000"> ∈{</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">1</span><span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000">}</span>

<span style="font-family:'DroidSans';font-size:5.98pt;color:#d9d9d9">Slide 12/86. «Java Memory Model Unlearning Experience», Aleksey Shipilёv, 2018, D:20180511133537+02’00’</span>

---

## Page 23

<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">JMM: Takeaway #1: Studying Implementations</span>

<span style="font-family:'DroidSans';font-size:5.98pt;color:#d9d9d9">Slide 13/86. «Java Memory Model Unlearning Experience», Aleksey Shipilёv, 2018, D:20180511133537+02’00’</span>

<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Implementations are allowed to</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">generate the</span><span style="font-family:'DroidSans-Bold';font-size:14.35pt;font-weight:bold;color:#000000"> subset</span><span style="font-family:'DroidSans';font-size:14.35pt;color:#000000"> of allowed</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">outcomes, not all of them</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">You can study JSR 133</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Cookbook, but take it with a</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">grain of salt</span>

![](./geecon-May2018-jmm_assets/images/p023_img01.png)

---

## Page 24

<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">JMM: Takeaway #1: Studying Implementations</span>

<span style="font-family:'DroidSans';font-size:5.98pt;color:#d9d9d9">Slide 13/86. «Java Memory Model Unlearning Experience», Aleksey Shipilёv, 2018, D:20180511133537+02’00’</span>

<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Implementations are allowed to</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">generate the</span><span style="font-family:'DroidSans-Bold';font-size:14.35pt;font-weight:bold;color:#000000"> subset</span><span style="font-family:'DroidSans';font-size:14.35pt;color:#000000"> of allowed</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">outcomes, not all of them</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">You can study JSR 133</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Cookbook, but take it with a</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">grain of salt</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Reductio ad absurdum:</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Global Interpreter Lock</span>

![](./geecon-May2018-jmm_assets/images/p024_img01.png)

---

## Page 25

<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">JMM: JSR 133 Cookbook For Compiler Writers!</span>
<span style="font-family:'F44';font-size:11.96pt;color:#0000ff">...</span>
<span style="font-family:'F44';font-size:11.96pt;color:#000000">volatile load</span>
<span style="font-family:'F44';font-size:11.96pt;color:#000000">[LoadLoad + LoadStore]</span>
<span style="font-family:'F44';font-size:11.96pt;color:#0000ff">...</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">«Oh wow, so simple!»</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">1. Do not push operations after the</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> volatile store</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">2. Do not pull operations before the</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> volatile load</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">3. Do (1), (2) for</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> synchronized enter</span><span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">/</span><span style="font-family:'F19';font-size:14.35pt;color:#000000">exit</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">4. Do not push operations after writing</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> final</span><span style="font-family:'DroidSans';font-size:14.35pt;color:#000000"> ﬁelds</span>
<span style="font-family:'DroidSans';font-size:5.98pt;color:#d9d9d9">Slide 14/86. «Java Memory Model Unlearning Experience», Aleksey Shipilёv, 2018, D:20180511133537+02’00’</span>

<span style="font-family:'F44';font-size:11.96pt;color:#0000ff">...</span>
<span style="font-family:'F44';font-size:11.96pt;color:#000000">[</span><span style="font-family:'F44';font-size:11.96pt;color:#1a177d">StoreStore + LoadStore</span><span style="font-family:'F44';font-size:11.96pt;color:#000000">]</span>
<span style="font-family:'F44';font-size:11.96pt;color:#000000">volatile store</span>
<span style="font-family:'F44';font-size:11.96pt;color:#000000">[StoreLoad]</span>
<span style="font-family:'F44';font-size:11.96pt;color:#0000ff">...</span>

---

## Page 26

<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">JMM: Lock Coarsening with JSR 133 Cookbook</span>
<span style="font-family:'F44';font-size:11.96pt;color:#b00040">void</span><span style="font-family:'F44';font-size:11.96pt;color:#0000ff"> m</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">() {</span>
<span style="font-family:'F44';font-size:11.96pt;color:#008000">synchronized</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">(</span><span style="font-family:'F44';font-size:11.96pt;color:#008000">this</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">) {</span>
<span style="font-family:'F44';font-size:11.96pt;color:#000000">x</span><span style="font-family:'F44';font-size:11.96pt;color:#666666"> = 1;</span>
<span style="font-family:'F44';font-size:11.96pt;color:#666666">}</span>
<span style="font-family:'F44';font-size:11.96pt;color:#008000">synchronized</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">(</span><span style="font-family:'F44';font-size:11.96pt;color:#008000">this</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">) {</span>

<span style="font-family:'DroidSans';font-size:7.97pt;color:#000000">Cookbook</span>
<span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000">−−−−−→</span>

<span style="font-family:'F44';font-size:11.96pt;color:#000000">y</span><span style="font-family:'F44';font-size:11.96pt;color:#666666"> = 1;</span>
<span style="font-family:'F44';font-size:11.96pt;color:#666666">}</span>
<span style="font-family:'F44';font-size:11.96pt;color:#666666">}</span>

<span style="font-family:'DroidSans';font-size:5.98pt;color:#d9d9d9">Slide 15/86. «Java Memory Model Unlearning Experience», Aleksey Shipilёv, 2018, D:20180511133537+02’00’</span>

<span style="font-family:'F44';font-size:11.96pt;color:#b00040">void</span><span style="font-family:'F44';font-size:11.96pt;color:#0000ff"> m</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">() {</span>
<span style="font-family:'F44';font-size:11.96pt;color:#666666">[</span><span style="font-family:'F44';font-size:11.96pt;color:#000000">LoadStore</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">]</span>
<span style="font-family:'F45';font-size:11.96pt;color:#408080">// monitorenter</span>
<span style="font-family:'F44';font-size:11.96pt;color:#000000">x</span><span style="font-family:'F44';font-size:11.96pt;color:#666666"> = 1;</span>
<span style="font-family:'F44';font-size:11.96pt;color:#666666">[</span><span style="font-family:'F44';font-size:11.96pt;color:#000000">StoreStore</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">]</span><span style="font-family:'F45';font-size:11.96pt;color:#408080"> // monitorexit</span>
<span style="font-family:'F44';font-size:11.96pt;color:#666666">[</span><span style="font-family:'F44';font-size:11.96pt;color:#000000">StoreLoad</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">]</span>
<span style="font-family:'F44';font-size:11.96pt;color:#666666">[</span><span style="font-family:'F44';font-size:11.96pt;color:#000000">LoadStore</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">]</span>
<span style="font-family:'F45';font-size:11.96pt;color:#408080">// monitorenter</span>
<span style="font-family:'F44';font-size:11.96pt;color:#000000">y</span><span style="font-family:'F44';font-size:11.96pt;color:#666666"> = 1;</span>
<span style="font-family:'F44';font-size:11.96pt;color:#666666">[</span><span style="font-family:'F44';font-size:11.96pt;color:#000000">StoreStore</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">]</span><span style="font-family:'F45';font-size:11.96pt;color:#408080"> // monitorexit</span>
<span style="font-family:'F44';font-size:11.96pt;color:#666666">[</span><span style="font-family:'F44';font-size:11.96pt;color:#000000">StoreLoad</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">]</span>
<span style="font-family:'F44';font-size:11.96pt;color:#666666">}</span>

<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Can we reorder</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> x = 1</span><span style="font-family:'DroidSans';font-size:14.35pt;color:#000000"> and</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> y = 1</span><span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">?</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">JSR 133 Cookbook: Nope, you cannot.</span>

---

## Page 27

<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">JMM: Lock Coarsening with JMM</span>
<span style="font-family:'F44';font-size:11.96pt;color:#b00040">void</span><span style="font-family:'F44';font-size:11.96pt;color:#0000ff"> m</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">() {</span>
<span style="font-family:'F44';font-size:11.96pt;color:#008000">synchronized</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">(</span><span style="font-family:'F44';font-size:11.96pt;color:#008000">this</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">) {</span>
<span style="font-family:'F44';font-size:11.96pt;color:#000000">x</span><span style="font-family:'F44';font-size:11.96pt;color:#666666"> = 1;</span>
<span style="font-family:'F44';font-size:11.96pt;color:#666666">}</span>
<span style="font-family:'F44';font-size:11.96pt;color:#008000">synchronized</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">(</span><span style="font-family:'F44';font-size:11.96pt;color:#008000">this</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">) {</span>

<span style="font-family:'DroidSans';font-size:7.97pt;color:#000000">coarsening</span>
<span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000">−−−−−→</span>

<span style="font-family:'F44';font-size:11.96pt;color:#000000">y</span><span style="font-family:'F44';font-size:11.96pt;color:#666666"> = 1;</span>
<span style="font-family:'F44';font-size:11.96pt;color:#666666">}</span>
<span style="font-family:'F44';font-size:11.96pt;color:#666666">}</span>

<span style="font-family:'DroidSans';font-size:5.98pt;color:#d9d9d9">Slide 16/86. «Java Memory Model Unlearning Experience», Aleksey Shipilёv, 2018, D:20180511133537+02’00’</span>

<span style="font-family:'F44';font-size:11.96pt;color:#b00040">void</span><span style="font-family:'F44';font-size:11.96pt;color:#0000ff"> m</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">() {</span>
<span style="font-family:'F44';font-size:11.96pt;color:#008000">synchronized</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">(</span><span style="font-family:'F44';font-size:11.96pt;color:#008000">this</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">) {</span>
<span style="font-family:'F44';font-size:11.96pt;color:#000000">y</span><span style="font-family:'F44';font-size:11.96pt;color:#666666"> = 1;</span>
<span style="font-family:'F44';font-size:11.96pt;color:#000000">x</span><span style="font-family:'F44';font-size:11.96pt;color:#666666"> = 1;</span>
<span style="font-family:'F44';font-size:11.96pt;color:#666666">}</span>
<span style="font-family:'F44';font-size:11.96pt;color:#666666">}</span>

<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">JSR 133 Cookbook: Nope, you cannot.</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Java Memory Model: Of course you can.</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">HotSpot: OK, doing it!</span>

---

## Page 28

<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">JMM: Takeaway #2, Implementation Details</span>

<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">These are not mandated by speciﬁcation,</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">these are implementation details:</span>
<span style="font-family:'F44';font-size:11.96pt;color:#b00040">void</span><span style="font-family:'F44';font-size:11.96pt;color:#0000ff"> barrier</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">() {</span>
<span style="font-family:'F44';font-size:11.96pt;color:#008000">synchronized</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">(</span><span style="font-family:'F44';font-size:11.96pt;color:#008000">this</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">) {};</span><span style="font-family:'F45';font-size:11.96pt;color:#408080"> // do barrier!</span>
<span style="font-family:'F44';font-size:11.96pt;color:#666666">}</span>

<span style="font-family:'DroidSans';font-size:5.98pt;color:#d9d9d9">Slide 17/86. «Java Memory Model Unlearning Experience», Aleksey Shipilёv, 2018, D:20180511133537+02’00’</span>

---

## Page 29

<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">JMM: Takeaway #2, Implementation Details</span>

<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">These are not mandated by speciﬁcation,</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">these are implementation details:</span>
<span style="font-family:'F44';font-size:11.96pt;color:#b00040">void</span><span style="font-family:'F44';font-size:11.96pt;color:#0000ff"> barrier</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">() {</span>
<span style="font-family:'F44';font-size:11.96pt;color:#008000">synchronized</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">(</span><span style="font-family:'F44';font-size:11.96pt;color:#008000">this</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">) {};</span><span style="font-family:'F45';font-size:11.96pt;color:#408080"> // do barrier!</span>
<span style="font-family:'F44';font-size:11.96pt;color:#666666">}</span>
<span style="font-family:'F44';font-size:11.96pt;color:#008000">volatile</span><span style="font-family:'F44';font-size:11.96pt;color:#b00040"> int</span><span style="font-family:'F44';font-size:11.96pt;color:#000000"> v</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">;</span>
<span style="font-family:'F44';font-size:11.96pt;color:#b00040">void</span><span style="font-family:'F44';font-size:11.96pt;color:#0000ff"> barrier</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">() {</span>
<span style="font-family:'F44';font-size:11.96pt;color:#000000">v</span><span style="font-family:'F44';font-size:11.96pt;color:#666666"> = 1;</span><span style="font-family:'F45';font-size:11.96pt;color:#408080"> // do barrier!</span>
<span style="font-family:'F44';font-size:11.96pt;color:#666666">}</span>
<span style="font-family:'DroidSans';font-size:5.98pt;color:#d9d9d9">Slide 17/86. «Java Memory Model Unlearning Experience», Aleksey Shipilёv, 2018, D:20180511133537+02’00’</span>

---

## Page 30

<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">JMM: Takeaway #2, Implementation Details</span>

<span style="font-family:'F44';font-size:11.96pt;color:#b00040">void</span><span style="font-family:'F44';font-size:11.96pt;color:#0000ff"> barrier</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">() {</span>
<span style="font-family:'F44';font-size:11.96pt;color:#008000">synchronized</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">(</span><span style="font-family:'F44';font-size:11.96pt;color:#008000">this</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">) {};</span><span style="font-family:'F45';font-size:11.96pt;color:#408080"> // do barrier!</span>
<span style="font-family:'F44';font-size:11.96pt;color:#666666">}</span>

<span style="font-family:'F44';font-size:11.96pt;color:#008000">volatile</span><span style="font-family:'F44';font-size:11.96pt;color:#b00040"> int</span><span style="font-family:'F44';font-size:11.96pt;color:#000000"> v</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">;</span>
<span style="font-family:'F44';font-size:11.96pt;color:#b00040">void</span><span style="font-family:'F44';font-size:11.96pt;color:#0000ff"> barrier</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">() {</span>
<span style="font-family:'F44';font-size:11.96pt;color:#008000">this</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">.</span><span style="font-family:'F44';font-size:11.96pt;color:#7d8f29">v</span><span style="font-family:'F44';font-size:11.96pt;color:#666666"> = 42;</span>
<span style="font-family:'F45';font-size:11.96pt;color:#408080">// do barrier!</span>
<span style="font-family:'F44';font-size:11.96pt;color:#666666">}</span>
<span style="font-family:'F44';font-size:11.96pt;color:#666666">}</span>
<span style="font-family:'DroidSans';font-size:5.98pt;color:#d9d9d9">Slide 17/86. «Java Memory Model Unlearning Experience», Aleksey Shipilёv, 2018, D:20180511133537+02’00’</span>
<span style="font-family:'F44';font-size:11.96pt;color:#000000">v</span><span style="font-family:'F44';font-size:11.96pt;color:#666666"> = 1;</span><span style="font-family:'F45';font-size:11.96pt;color:#408080"> // do barrier!</span>
<span style="font-family:'F44';font-size:11.96pt;color:#666666">}</span>

<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">These are not mandated by speciﬁcation,</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">these are implementation details:</span>

<span style="font-family:'F44';font-size:11.96pt;color:#008000">class</span><span style="font-family:'F44';font-size:11.96pt;color:#0000ff"> MyClass</span><span style="font-family:'F44';font-size:11.96pt;color:#666666"> {</span>
<span style="font-family:'F44';font-size:11.96pt;color:#008000">volatile</span><span style="font-family:'F44';font-size:11.96pt;color:#b00040"> int</span><span style="font-family:'F44';font-size:11.96pt;color:#000000"> v</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">;</span>
<span style="font-family:'F44';font-size:11.96pt;color:#000000">MyClass</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">() {</span>

---

## Page 31

Behaviors

<!-- 图源: ./geecon-May2018-jmm_assets/images/p031_scan.jpg -->


<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">Behaviors</span>

---

## Page 32

<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">Races: Example 1.1</span>

<span style="font-family:'F19';font-size:14.35pt;color:#000000">class M { ... }</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">M</span><span style="font-family:'F19';font-size:14.35pt;color:#ff0000"> m</span><span style="font-family:'F19';font-size:14.35pt;color:#000000">;</span>
<span style="font-family:'F19';font-size:14.35pt;color:#ff0000">m</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> = new M();</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">M</span><span style="font-family:'F19';font-size:14.35pt;color:#0000ff"> lm</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> =</span><span style="font-family:'F19';font-size:14.35pt;color:#ff0000"> m</span><span style="font-family:'F19';font-size:14.35pt;color:#000000">;</span>
<span style="font-family:'F19';font-size:14.35pt;color:#ff0000">m</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> = null;</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">r1 = (</span><span style="font-family:'F19';font-size:14.35pt;color:#0000ff">lm</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> != null);</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">r2 = (</span><span style="font-family:'F19';font-size:14.35pt;color:#0000ff">lm</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> != null);</span>

<span style="font-family:'DroidSans';font-size:5.98pt;color:#d9d9d9">Slide 19/86. «Java Memory Model Unlearning Experience», Aleksey Shipilёv, 2018, D:20180511133537+02’00’</span>

---

## Page 33

<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">Races: Example 1.1</span>

<span style="font-family:'F19';font-size:14.35pt;color:#000000">class M { ... }</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">M</span><span style="font-family:'F19';font-size:14.35pt;color:#ff0000"> m</span><span style="font-family:'F19';font-size:14.35pt;color:#000000">;</span>
<span style="font-family:'F19';font-size:14.35pt;color:#ff0000">m</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> = new M();</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">M</span><span style="font-family:'F19';font-size:14.35pt;color:#0000ff"> lm</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> =</span><span style="font-family:'F19';font-size:14.35pt;color:#ff0000"> m</span><span style="font-family:'F19';font-size:14.35pt;color:#000000">;</span>
<span style="font-family:'F19';font-size:14.35pt;color:#ff0000">m</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> = null;</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">r1 = (</span><span style="font-family:'F19';font-size:14.35pt;color:#0000ff">lm</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> != null);</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">r2 = (</span><span style="font-family:'F19';font-size:14.35pt;color:#0000ff">lm</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> != null);</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">JMM allows only</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000"> (</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝐹, 𝐹</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">)</span><span style="font-family:'DroidSans';font-size:14.35pt;color:#000000"> and</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000"> (</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑇, 𝑇</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">)</span>

<span style="font-family:'DroidSans';font-size:5.98pt;color:#d9d9d9">Slide 19/86. «Java Memory Model Unlearning Experience», Aleksey Shipilёv, 2018, D:20180511133537+02’00’</span>

---

## Page 34

<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">Races: Example 1.1, Counter-Argument</span>

<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Can’t compiler «inline» the local variable?</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">class M { ... }</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">M</span><span style="font-family:'F19';font-size:14.35pt;color:#ff0000"> m</span><span style="font-family:'F19';font-size:14.35pt;color:#000000">;</span>
<span style="font-family:'F19';font-size:14.35pt;color:#ff0000">m</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> = new M();</span>
<span style="font-family:'F19';font-size:14.35pt;color:#ff0000">m</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> = null;</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">r1 = (</span><span style="font-family:'F19';font-size:14.35pt;color:#ff0000">m</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> != null);</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">r2 = (</span><span style="font-family:'F19';font-size:14.35pt;color:#ff0000">m</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> != null);</span>

<span style="font-family:'DroidSans';font-size:5.98pt;color:#d9d9d9">Slide 20/86. «Java Memory Model Unlearning Experience», Aleksey Shipilёv, 2018, D:20180511133537+02’00’</span>

---

## Page 35

<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">Races: Example 1.1, Counter-Argument</span>

<span style="font-family:'F19';font-size:14.35pt;color:#000000">class M { ... }</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">M</span><span style="font-family:'F19';font-size:14.35pt;color:#ff0000"> m</span><span style="font-family:'F19';font-size:14.35pt;color:#000000">;</span>
<span style="font-family:'F19';font-size:14.35pt;color:#ff0000">m</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> = new M();</span>
<span style="font-family:'F19';font-size:14.35pt;color:#ff0000">m</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> = null;</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">r1 = (</span><span style="font-family:'F19';font-size:14.35pt;color:#ff0000">m</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> != null);</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">r2 = (</span><span style="font-family:'F19';font-size:14.35pt;color:#ff0000">m</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> != null);</span>

<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">See, there is an obvious execution that yields</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000"> (</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑇, 𝐹</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">)</span><span style="font-family:'DroidSans';font-size:14.35pt;color:#000000"> now!</span>

<span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">... 𝑟</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑚</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">) : !</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑛𝑢𝑙𝑙</span>

<span style="font-family:'DroidSans';font-size:5.98pt;color:#d9d9d9">Slide 20/86. «Java Memory Model Unlearning Experience», Aleksey Shipilёv, 2018, D:20180511133537+02’00’</span>

<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Can’t compiler «inline» the local variable?</span>

<span style="font-family:'DroidSans';font-size:7.97pt;color:#000000">po</span>
<span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000">−−→</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑟</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑚</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">) :</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000"> 𝑛𝑢𝑙𝑙</span>

---

## Page 36

<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">JMM: Program Order</span>

<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Program order ( PO ) provides the link</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">between the execution and the program in question</span>

<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">PO – total order for any given thread in isolation</span>
<span style="font-family:'DroidSans-Bold';font-size:14.35pt;font-weight:bold;color:#000000">PO consistency</span><span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">: PO is consistent with the source</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">code order in the original program</span>

<span style="font-family:'DroidSans';font-size:5.98pt;color:#d9d9d9">Slide 21/86. «Java Memory Model Unlearning Experience», Aleksey Shipilёv, 2018, D:20180511133537+02’00’</span>

---

## Page 37

<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">JMM: PO And Transformations</span>

<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Original program:</span>

<span style="font-family:'F19';font-size:14.35pt;color:#000000">M</span><span style="font-family:'F19';font-size:14.35pt;color:#0000ff"> lm</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> =</span><span style="font-family:'F19';font-size:14.35pt;color:#ff0000"> m</span><span style="font-family:'F19';font-size:14.35pt;color:#000000">;</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">r1 = (</span><span style="font-family:'F19';font-size:14.35pt;color:#0000ff">lm</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> != null);</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">r2 = (</span><span style="font-family:'F19';font-size:14.35pt;color:#0000ff">lm</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> != null);</span>
<span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑤</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑚,</span><span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000"> *</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">)</span>

<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Transformed program:</span>

<span style="font-family:'F19';font-size:14.35pt;color:#000000">r1 = (</span><span style="font-family:'F19';font-size:14.35pt;color:#ff0000">m</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> != null);</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">r2 = (</span><span style="font-family:'F19';font-size:14.35pt;color:#ff0000">m</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> != null);</span>
<span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑤</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑚,</span><span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000"> *</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">)</span>
<span style="font-family:'DroidSans';font-size:5.98pt;color:#d9d9d9">Slide 22/86. «Java Memory Model Unlearning Experience», Aleksey Shipilёv, 2018, D:20180511133537+02’00’</span>

<span style="font-family:'DroidSans';font-size:7.97pt;color:#000000">po</span>
<span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000">−−→</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑤</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑚, 𝑛𝑢𝑙𝑙</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">)</span>
<span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑟</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑚</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">) :</span><span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000"> *</span>

<span style="font-family:'DroidSans';font-size:7.97pt;color:#000000">po</span>
<span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000">−−→</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑤</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑚, 𝑛𝑢𝑙𝑙</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">)</span>
<span style="font-family:'DroidSans';font-size:7.97pt;color:#000000">po</span>
<span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000">−−→</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑟</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑚</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">) :</span><span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000"> *</span>

<span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑟</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑚</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">) :</span><span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000"> *</span>

---

## Page 38

<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">JMM: PO And Transformations</span>

<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Original program:</span>

<span style="font-family:'F19';font-size:14.35pt;color:#000000">M</span><span style="font-family:'F19';font-size:14.35pt;color:#0000ff"> lm</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> =</span><span style="font-family:'F19';font-size:14.35pt;color:#ff0000"> m</span><span style="font-family:'F19';font-size:14.35pt;color:#000000">;</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">r1 = (</span><span style="font-family:'F19';font-size:14.35pt;color:#0000ff">lm</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> != null);</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">r2 = (</span><span style="font-family:'F19';font-size:14.35pt;color:#0000ff">lm</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> != null);</span>
<span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑤</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑚,</span><span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000"> *</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">)</span>

<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Transformed program:</span>

<span style="font-family:'F19';font-size:14.35pt;color:#000000">r1 = (</span><span style="font-family:'F19';font-size:14.35pt;color:#ff0000">m</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> != null);</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">r2 = (</span><span style="font-family:'F19';font-size:14.35pt;color:#ff0000">m</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> != null);</span>
<span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑤</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑚,</span><span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000"> *</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">)</span>
<span style="font-family:'DroidSans';font-size:5.98pt;color:#d9d9d9">Slide 22/86. «Java Memory Model Unlearning Experience», Aleksey Shipilёv, 2018, D:20180511133537+02’00’</span>

<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">This execution does not relate</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">to the original program, oops</span>

<span style="font-family:'DroidSans';font-size:7.97pt;color:#000000">po</span>
<span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000">−−→</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑤</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑚, 𝑛𝑢𝑙𝑙</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">)</span>
<span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑟</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑚</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">) :</span><span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000"> *</span>

<span style="font-family:'DroidSans';font-size:7.97pt;color:#000000">po</span>
<span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000">−−→</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑤</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑚, 𝑛𝑢𝑙𝑙</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">)</span>
<span style="font-family:'DroidSans';font-size:7.97pt;color:#000000">po</span>
<span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000">−−→</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑟</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑚</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">) :</span><span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000"> *</span>

<span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑟</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑚</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">) :</span><span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000"> *</span>

---

## Page 39

<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">JMM: PO And Transformations</span>

<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Original program:</span>

<span style="font-family:'F19';font-size:14.35pt;color:#000000">M</span><span style="font-family:'F19';font-size:14.35pt;color:#0000ff"> lm</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> =</span><span style="font-family:'F19';font-size:14.35pt;color:#ff0000"> m</span><span style="font-family:'F19';font-size:14.35pt;color:#000000">;</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">r1 = (</span><span style="font-family:'F19';font-size:14.35pt;color:#0000ff">lm</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> != null);</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">r2 = (</span><span style="font-family:'F19';font-size:14.35pt;color:#0000ff">lm</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> != null);</span>
<span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑤</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑚,</span><span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000"> *</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">)</span>

<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Transformed program:</span>

<span style="font-family:'F19';font-size:14.35pt;color:#000000">r1 = (</span><span style="font-family:'F19';font-size:14.35pt;color:#ff0000">m</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> != null);</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">r2 = (</span><span style="font-family:'F19';font-size:14.35pt;color:#ff0000">m</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> != null);</span>
<span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑤</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑚,</span><span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000"> *</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">)</span>
<span style="font-family:'DroidSans';font-size:5.98pt;color:#d9d9d9">Slide 22/86. «Java Memory Model Unlearning Experience», Aleksey Shipilёv, 2018, D:20180511133537+02’00’</span>

<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">This execution should be used</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">to reason about outcomes</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">for the transformed program</span>
<span style="font-family:'DroidSans';font-size:7.97pt;color:#000000">po</span>
<span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000">−−→</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑤</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑚, 𝑛𝑢𝑙𝑙</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">)</span>
<span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑟</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑚</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">) :</span><span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000"> *</span>

<span style="font-family:'DroidSans';font-size:7.97pt;color:#000000">po</span>
<span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000">−−→</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑤</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑚, 𝑛𝑢𝑙𝑙</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">)</span>
<span style="font-family:'DroidSans';font-size:7.97pt;color:#000000">po</span>
<span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000">−−→</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑟</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑚</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">) :</span><span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000"> *</span>

<span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑟</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑚</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">) :</span><span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000"> *</span>

---

## Page 40

<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">JMM: PO And Transformations</span>

<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Original program:</span>

<span style="font-family:'F19';font-size:14.35pt;color:#000000">M</span><span style="font-family:'F19';font-size:14.35pt;color:#0000ff"> lm</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> =</span><span style="font-family:'F19';font-size:14.35pt;color:#ff0000"> m</span><span style="font-family:'F19';font-size:14.35pt;color:#000000">;</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">r1 = (</span><span style="font-family:'F19';font-size:14.35pt;color:#0000ff">lm</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> != null);</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">r2 = (</span><span style="font-family:'F19';font-size:14.35pt;color:#0000ff">lm</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> != null);</span>
<span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑤</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑚,</span><span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000"> *</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">)</span>

<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Transformed program:</span>

<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">PO consistency:</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Original program has single read?</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Relatable executions also have single read!</span>

<span style="font-family:'F19';font-size:14.35pt;color:#000000">r1 = (</span><span style="font-family:'F19';font-size:14.35pt;color:#ff0000">m</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> != null);</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">r2 = (</span><span style="font-family:'F19';font-size:14.35pt;color:#ff0000">m</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> != null);</span>
<span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑤</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑚,</span><span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000"> *</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">)</span>
<span style="font-family:'DroidSans';font-size:5.98pt;color:#d9d9d9">Slide 22/86. «Java Memory Model Unlearning Experience», Aleksey Shipilёv, 2018, D:20180511133537+02’00’</span>

<span style="font-family:'DroidSans';font-size:7.97pt;color:#000000">po</span>
<span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000">−−→</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑤</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑚, 𝑛𝑢𝑙𝑙</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">)</span>
<span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑟</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑚</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">) :</span><span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000"> *</span>

<span style="font-family:'DroidSans';font-size:7.97pt;color:#000000">po</span>
<span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000">−−→</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑤</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑚, 𝑛𝑢𝑙𝑙</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">)</span>
<span style="font-family:'DroidSans';font-size:7.97pt;color:#000000">po</span>
<span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000">−−→</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑟</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑚</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">) :</span><span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000"> *</span>

<span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑟</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑚</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">) :</span><span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000"> *</span>

---

## Page 41

<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">Races: Example 1.2, Null-Checks</span>

<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">In Java, unlike C/C++:</span>

<span style="font-family:'F44';font-size:11.96pt;color:#b00040">int</span><span style="font-family:'F44';font-size:11.96pt;color:#0000ff"> s</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">() {</span>
<span style="font-family:'F44';font-size:11.96pt;color:#000000">M lm</span><span style="font-family:'F44';font-size:11.96pt;color:#666666"> =</span><span style="font-family:'F44';font-size:11.96pt;color:#000000"> m</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">;</span>
<span style="font-family:'F44';font-size:11.96pt;color:#008000">if</span><span style="font-family:'F44';font-size:11.96pt;color:#666666"> (</span><span style="font-family:'F44';font-size:11.96pt;color:#000000">lm</span><span style="font-family:'F44';font-size:11.96pt;color:#666666"> !=</span><span style="font-family:'F44';font-size:11.96pt;color:#008000"> null</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">) {</span>
<span style="font-family:'F44';font-size:11.96pt;color:#008000">return</span><span style="font-family:'F44';font-size:11.96pt;color:#000000"> lm</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">.</span><span style="font-family:'F44';font-size:11.96pt;color:#7d8f29">x</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">;</span><span style="font-family:'F45';font-size:11.96pt;color:#408080"> // &lt;--- This does not risk NPE</span>
<span style="font-family:'F44';font-size:11.96pt;color:#008000">else</span>
<span style="font-family:'F44';font-size:11.96pt;color:#008000">return</span><span style="font-family:'F44';font-size:11.96pt;color:#666666"> 0;</span>
<span style="font-family:'F44';font-size:11.96pt;color:#666666">}</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">This would later become a building block</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">for so called «benign» data races</span>

<span style="font-family:'DroidSans';font-size:5.98pt;color:#d9d9d9">Slide 23/86. «Java Memory Model Unlearning Experience», Aleksey Shipilёv, 2018, D:20180511133537+02’00’</span>

---

## Page 42

<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">Races: Takeaway #3</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">1. Data race behavior is still somewhat deterministic</span>

<span style="font-family:'DroidSans';font-size:11.96pt;color:#000000">Racy reads are stronger than in other languages</span>
<span style="font-family:'DroidSans';font-size:11.96pt;color:#000000">Weird stuff still happens, but not completely catastrophic</span>

<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">2. Memory-model-wise, there is a difference:</span>

<span style="font-family:'F44';font-size:11.96pt;color:#b00040">int</span><span style="font-family:'F44';font-size:11.96pt;color:#0000ff"> m1</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">() {</span>
<span style="font-family:'F44';font-size:11.96pt;color:#b00040">int</span><span style="font-family:'F44';font-size:11.96pt;color:#000000"> x1</span><span style="font-family:'F44';font-size:11.96pt;color:#666666"> =</span><span style="font-family:'F44';font-size:11.96pt;color:#000000"> field</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">;</span>
<span style="font-family:'F44';font-size:11.96pt;color:#b00040">int</span><span style="font-family:'F44';font-size:11.96pt;color:#000000"> x2</span><span style="font-family:'F44';font-size:11.96pt;color:#666666"> =</span><span style="font-family:'F44';font-size:11.96pt;color:#000000"> field</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">;</span>
<span style="font-family:'F44';font-size:11.96pt;color:#008000">return</span><span style="font-family:'F44';font-size:11.96pt;color:#000000"> x1</span><span style="font-family:'F44';font-size:11.96pt;color:#666666"> +</span><span style="font-family:'F44';font-size:11.96pt;color:#000000"> x2</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">;</span>
<span style="font-family:'F44';font-size:11.96pt;color:#666666">}</span>

<span style="font-family:'DroidSans';font-size:5.98pt;color:#d9d9d9">Slide 24/86. «Java Memory Model Unlearning Experience», Aleksey Shipilёv, 2018, D:20180511133537+02’00’</span>

<span style="font-family:'F44';font-size:11.96pt;color:#b00040">int</span><span style="font-family:'F44';font-size:11.96pt;color:#0000ff"> m2</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">() {</span>
<span style="font-family:'F44';font-size:11.96pt;color:#b00040">int</span><span style="font-family:'F44';font-size:11.96pt;color:#000000"> x1</span><span style="font-family:'F44';font-size:11.96pt;color:#666666"> =</span><span style="font-family:'F44';font-size:11.96pt;color:#000000"> field</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">;</span>
<span style="font-family:'F44';font-size:11.96pt;color:#b00040">int</span><span style="font-family:'F44';font-size:11.96pt;color:#000000"> x2</span><span style="font-family:'F44';font-size:11.96pt;color:#666666"> =</span><span style="font-family:'F44';font-size:11.96pt;color:#000000"> x1</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">;</span>
<span style="font-family:'F44';font-size:11.96pt;color:#008000">return</span><span style="font-family:'F44';font-size:11.96pt;color:#000000"> x1</span><span style="font-family:'F44';font-size:11.96pt;color:#666666"> +</span><span style="font-family:'F44';font-size:11.96pt;color:#000000"> x2</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">;</span>
<span style="font-family:'F44';font-size:11.96pt;color:#666666">}</span>

---

## Page 43

<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">Races: JMM and Ordering Modes</span>

<span style="font-family:'F19';font-size:14.35pt;color:#000000">Java 8</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">Java 9</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">–</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">–</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">N</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">plain</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">VH Plain</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">Y</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">volatile</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">VH SeqCst</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">Y</span>

<span style="font-family:'DroidSans';font-size:5.98pt;color:#d9d9d9">Slide 25/86. «Java Memory Model Unlearning Experience», Aleksey Shipilёv, 2018, D:20180511133537+02’00’</span>

<span style="font-family:'F19';font-size:14.35pt;color:#000000">Definite</span>

---

## Page 44

Coherence

<!-- 图源: ./geecon-May2018-jmm_assets/images/p044_scan.jpg -->


<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">Coherence</span>

---

## Page 45

<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">Coherence: Example 2.1</span>

<span style="font-family:'F19';font-size:14.35pt;color:#000000">int</span><span style="font-family:'F19';font-size:14.35pt;color:#0000ff"> x</span><span style="font-family:'F19';font-size:14.35pt;color:#000000">;</span>
<span style="font-family:'F19';font-size:14.35pt;color:#0000ff">x</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> = 1;</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">r1 =</span><span style="font-family:'F19';font-size:14.35pt;color:#0000ff"> x</span><span style="font-family:'F19';font-size:14.35pt;color:#000000">; //</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000"> 𝑟</span><span style="font-family:'CMR10';font-size:9.96pt;color:#000000">1</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">r2 =</span><span style="font-family:'F19';font-size:14.35pt;color:#0000ff"> x</span><span style="font-family:'F19';font-size:14.35pt;color:#000000">; //</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000"> 𝑟</span><span style="font-family:'CMR10';font-size:9.96pt;color:#000000">2</span>

<span style="font-family:'DroidSans';font-size:5.98pt;color:#d9d9d9">Slide 27/86. «Java Memory Model Unlearning Experience», Aleksey Shipilёv, 2018, D:20180511133537+02’00’</span>

---

## Page 46

<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">Coherence: Example 2.1</span>

<span style="font-family:'F19';font-size:14.35pt;color:#000000">int</span><span style="font-family:'F19';font-size:14.35pt;color:#0000ff"> x</span><span style="font-family:'F19';font-size:14.35pt;color:#000000">;</span>
<span style="font-family:'F19';font-size:14.35pt;color:#0000ff">x</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> = 1;</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">r1 =</span><span style="font-family:'F19';font-size:14.35pt;color:#0000ff"> x</span><span style="font-family:'F19';font-size:14.35pt;color:#000000">; //</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000"> 𝑟</span><span style="font-family:'CMR10';font-size:9.96pt;color:#000000">1</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">r2 =</span><span style="font-family:'F19';font-size:14.35pt;color:#0000ff"> x</span><span style="font-family:'F19';font-size:14.35pt;color:#000000">; //</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000"> 𝑟</span><span style="font-family:'CMR10';font-size:9.96pt;color:#000000">2</span>

<span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑤</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑥,</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000"> 1)</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000"> ... 𝑟</span><span style="font-family:'CMR10';font-size:9.96pt;color:#000000">1</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑥</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">) : 1</span>

<span style="font-family:'DroidSans';font-size:5.98pt;color:#d9d9d9">Slide 27/86. «Java Memory Model Unlearning Experience», Aleksey Shipilёv, 2018, D:20180511133537+02’00’</span>

<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">JMM allows observing</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000"> (1</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">,</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000"> 0)</span><span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">, see:</span>

<span style="font-family:'DroidSans';font-size:7.97pt;color:#000000">po</span>
<span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000">−−→</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑟</span><span style="font-family:'CMR10';font-size:9.96pt;color:#000000">2</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑥</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">) : 0</span>

---

## Page 47

<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">Coherence: Example 2.1</span>

<span style="font-family:'F19';font-size:14.35pt;color:#000000">int</span><span style="font-family:'F19';font-size:14.35pt;color:#0000ff"> x</span><span style="font-family:'F19';font-size:14.35pt;color:#000000">;</span>
<span style="font-family:'F19';font-size:14.35pt;color:#0000ff">x</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> = 1;</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">r1 =</span><span style="font-family:'F19';font-size:14.35pt;color:#0000ff"> x</span><span style="font-family:'F19';font-size:14.35pt;color:#000000">; //</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000"> 𝑟</span><span style="font-family:'CMR10';font-size:9.96pt;color:#000000">1</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">r2 =</span><span style="font-family:'F19';font-size:14.35pt;color:#0000ff"> x</span><span style="font-family:'F19';font-size:14.35pt;color:#000000">; //</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000"> 𝑟</span><span style="font-family:'CMR10';font-size:9.96pt;color:#000000">2</span>

<span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑤</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑥,</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000"> 1)</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000"> ... 𝑟</span><span style="font-family:'CMR10';font-size:9.96pt;color:#000000">1</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑥</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">) : 1</span>

<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">This execution is PO consistent, both reads are here!</span>
<span style="font-family:'DroidSans';font-size:5.98pt;color:#d9d9d9">Slide 27/86. «Java Memory Model Unlearning Experience», Aleksey Shipilёv, 2018, D:20180511133537+02’00’</span>

<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">JMM allows observing</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000"> (1</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">,</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000"> 0)</span><span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">, see:</span>

<span style="font-family:'DroidSans';font-size:7.97pt;color:#000000">po</span>
<span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000">−−→</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑟</span><span style="font-family:'CMR10';font-size:9.96pt;color:#000000">2</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑥</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">) : 0</span>

---

## Page 48

<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">Coherence: Deﬁnition</span>

<span style="font-family:'DroidSans-Bold';font-size:14.35pt;font-weight:bold;color:#000000">Coherence</span><span style="font-family:'DroidSans-Slant_213';font-size:14.35pt;font-style:italic;color:#000000"> (def.)</span><span style="font-family:'DroidSans';font-size:14.35pt;color:#000000"> :</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">The writes to the single memory location</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">appear to be in a total order</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">consistent with program order</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Most hardware gives this for free</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Most optimizers give up on this by default (i.e. do</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">not track the order of reads)</span>

<span style="font-family:'DroidSans';font-size:5.98pt;color:#d9d9d9">Slide 28/86. «Java Memory Model Unlearning Experience», Aleksey Shipilёv, 2018, D:20180511133537+02’00’</span>

---

## Page 49

<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">JMM: Consistency Rules</span>

<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">PO consistency affects the</span><span style="font-family:'DroidSans-Bold';font-size:14.35pt;font-weight:bold;color:#000000"> structure</span><span style="font-family:'DroidSans';font-size:14.35pt;color:#000000"> of the execution.</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">What we need: a consistency rule that affects</span><span style="font-family:'DroidSans-Bold';font-size:14.35pt;font-weight:bold;color:#000000"> values</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">observed by the actions.</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">In JMM, there are two of them:</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">1. Happens-before ( HB ) consistency</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">2. Synchronization order ( SO ) consistency</span>
<span style="font-family:'DroidSans';font-size:5.98pt;color:#d9d9d9">Slide 29/86. «Java Memory Model Unlearning Experience», Aleksey Shipilёv, 2018, D:20180511133537+02’00’</span>

---

## Page 50

<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">JMM: Consistency Rules</span>

<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">PO consistency affects the</span><span style="font-family:'DroidSans-Bold';font-size:14.35pt;font-weight:bold;color:#000000"> structure</span><span style="font-family:'DroidSans';font-size:14.35pt;color:#000000"> of the execution.</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">What we need: a consistency rule that affects</span><span style="font-family:'DroidSans-Bold';font-size:14.35pt;font-weight:bold;color:#000000"> values</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">observed by the actions.</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">In JMM, there are two of them:</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">1. Happens-before ( HB ) consistency</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">2. Synchronization order ( SO ) consistency</span><span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000"> ←</span><span style="font-family:'DroidSans-Bold';font-size:14.35pt;font-weight:bold;color:#000000">now!</span>
<span style="font-family:'DroidSans';font-size:5.98pt;color:#d9d9d9">Slide 29/86. «Java Memory Model Unlearning Experience», Aleksey Shipilёv, 2018, D:20180511133537+02’00’</span>

---

## Page 51

<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">JMM: SO – Synchronization Order</span>

<span style="font-family:'DroidSans-Bold';font-size:14.35pt;font-weight:bold;color:#000000">SO</span><span style="font-family:'DroidSans';font-size:14.35pt;color:#000000"> is a total order («All SA actions relate to each other»)</span>
<span style="font-family:'DroidSans-Bold';font-size:14.35pt;font-weight:bold;color:#000000">SO - PO consistency</span><span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">:</span>
<span style="font-family:'DroidSans-Bold';font-size:14.35pt;font-weight:bold;color:#000000">SO consistency</span><span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">: reads see only the latest write in</span>

<span style="font-family:'DroidSans';font-size:5.98pt;color:#d9d9d9">Slide 30/86. «Java Memory Model Unlearning Experience», Aleksey Shipilёv, 2018, D:20180511133537+02’00’</span>

<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">SO covers all</span><span style="font-family:'DroidSans-Slant_213';font-size:14.35pt;font-style:italic;color:#000000"> synchronization actions</span><span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">:</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">volatile read/write, lock/unlock, etc.</span>

<span style="font-family:'DroidSans';font-size:7.97pt;color:#000000">po</span>
<span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000">−−→</span><span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">agree</span>

<span style="font-family:'DroidSans';font-size:7.97pt;color:#000000">so</span>
<span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000">−−→</span><span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">and</span>

<span style="font-family:'DroidSans';font-size:7.97pt;color:#000000">so</span>
<span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000">−−→</span>

---

## Page 52

<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">JMM: SO – Synchronization Order</span>

<span style="font-family:'DroidSans-Bold';font-size:14.35pt;font-weight:bold;color:#000000">SO</span><span style="font-family:'DroidSans';font-size:14.35pt;color:#000000"> is a total order («All SA actions relate to each other»)</span>
<span style="font-family:'DroidSans-Bold';font-size:14.35pt;font-weight:bold;color:#000000">SO - PO consistency</span><span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">:</span>
<span style="font-family:'DroidSans-Bold';font-size:14.35pt;font-weight:bold;color:#000000">SO consistency</span><span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">: reads see only the latest write in</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Just what coherence wants!</span>
<span style="font-family:'DroidSans';font-size:5.98pt;color:#d9d9d9">Slide 30/86. «Java Memory Model Unlearning Experience», Aleksey Shipilёv, 2018, D:20180511133537+02’00’</span>

<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">SO covers all</span><span style="font-family:'DroidSans-Slant_213';font-size:14.35pt;font-style:italic;color:#000000"> synchronization actions</span><span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">:</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">volatile read/write, lock/unlock, etc.</span>

<span style="font-family:'DroidSans';font-size:7.97pt;color:#000000">po</span>
<span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000">−−→</span><span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">agree</span>

<span style="font-family:'DroidSans';font-size:7.97pt;color:#000000">so</span>
<span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000">−−→</span><span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">and</span>

<span style="font-family:'DroidSans';font-size:7.97pt;color:#000000">so</span>
<span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000">−−→</span>

---

## Page 53

<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">Coherence: Example 2.2</span>

<span style="font-family:'F19';font-size:14.35pt;color:#000000">volatile int</span><span style="font-family:'F19';font-size:14.35pt;color:#ff0000"> x</span><span style="font-family:'F19';font-size:14.35pt;color:#000000">;</span>
<span style="font-family:'F19';font-size:14.35pt;color:#ff0000">x</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> = 1;</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">r1 =</span><span style="font-family:'F19';font-size:14.35pt;color:#ff0000"> x</span><span style="font-family:'F19';font-size:14.35pt;color:#000000">; //</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000"> 𝑟</span><span style="font-family:'CMR10';font-size:9.96pt;color:#000000">1</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">r2 =</span><span style="font-family:'F19';font-size:14.35pt;color:#ff0000"> x</span><span style="font-family:'F19';font-size:14.35pt;color:#000000">; //</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000"> 𝑟</span><span style="font-family:'CMR10';font-size:9.96pt;color:#000000">2</span>

<span style="font-family:'DroidSans';font-size:5.98pt;color:#d9d9d9">Slide 31/86. «Java Memory Model Unlearning Experience», Aleksey Shipilёv, 2018, D:20180511133537+02’00’</span>

---

## Page 54

<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">Coherence: Example 2.2</span>

<span style="font-family:'F19';font-size:14.35pt;color:#000000">volatile int</span><span style="font-family:'F19';font-size:14.35pt;color:#ff0000"> x</span><span style="font-family:'F19';font-size:14.35pt;color:#000000">;</span>
<span style="font-family:'F19';font-size:14.35pt;color:#ff0000">x</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> = 1;</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">r1 =</span><span style="font-family:'F19';font-size:14.35pt;color:#ff0000"> x</span><span style="font-family:'F19';font-size:14.35pt;color:#000000">; //</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000"> 𝑟</span><span style="font-family:'CMR10';font-size:9.96pt;color:#000000">1</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">r2 =</span><span style="font-family:'F19';font-size:14.35pt;color:#ff0000"> x</span><span style="font-family:'F19';font-size:14.35pt;color:#000000">; //</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000"> 𝑟</span><span style="font-family:'CMR10';font-size:9.96pt;color:#000000">2</span>

<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Valid executions give</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000"> (0</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">,</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000"> 0)</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">,</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000"> (1</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">,</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000"> 1)</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">,</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000"> (0</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">,</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000"> 1)</span><span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">:</span><span style="font-family:'DroidSans-Slant_213';font-size:9.96pt;font-style:italic;color:#000000">a</span>

<span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑤</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑥,</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000"> 1)</span>
<span style="font-family:'DroidSans';font-size:7.97pt;color:#000000">so</span>
<span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000">−−→</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑟</span><span style="font-family:'CMR10';font-size:9.96pt;color:#000000">1</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑥</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">) : 1</span>
<span style="font-family:'DroidSans';font-size:7.97pt;color:#000000">so</span>
<span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000">−−→</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑟</span><span style="font-family:'CMR10';font-size:9.96pt;color:#000000">2</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑥</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">) : 1</span>
<span style="font-family:'DroidSans';font-size:7.97pt;color:#000000">so</span>
<span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000">−−→</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑟</span><span style="font-family:'CMR10';font-size:9.96pt;color:#000000">2</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑥</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">) : 1</span>
<span style="font-family:'DroidSans';font-size:7.97pt;color:#000000">so</span>
<span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000">−−→</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑤</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑥,</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000"> 1)</span>

<span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑟</span><span style="font-family:'CMR10';font-size:9.96pt;color:#000000">1</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑥</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">) : 0</span>
<span style="font-family:'DroidSans';font-size:7.97pt;color:#000000">so</span>
<span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000">−−→</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑤</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑥,</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000"> 1)</span>

<span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑟</span><span style="font-family:'CMR10';font-size:9.96pt;color:#000000">1</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑥</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">) : 0</span>
<span style="font-family:'DroidSans';font-size:7.97pt;color:#000000">so</span>
<span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000">−−→</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑟</span><span style="font-family:'CMR10';font-size:9.96pt;color:#000000">2</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑥</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">) : 0</span>

<span style="font-family:'DroidSans-Slant_213';font-size:6.97pt;font-style:italic;color:#000000">a</span><span style="font-family:'DroidSans';font-size:9.96pt;color:#000000">Proving no other outcomes exist is left as an exercise for the reader</span>
<span style="font-family:'DroidSans';font-size:5.98pt;color:#d9d9d9">Slide 31/86. «Java Memory Model Unlearning Experience», Aleksey Shipilёv, 2018, D:20180511133537+02’00’</span>

---

## Page 55

<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">Coherence: Takeaway #4</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">1. Races laugh at our presuppositions about order</span>

<span style="font-family:'DroidSans';font-size:11.96pt;color:#000000">Most of the time, there is a complete free-for-all</span>
<span style="font-family:'DroidSans';font-size:11.96pt;color:#000000">Madness usually manifests after code transformations</span>
<span style="font-family:'DroidSans';font-size:11.96pt;color:#000000">Although hardware can also get us down</span>

<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">2. Coherency, while basic, is not guaranteed, unless...</span>

<span style="font-family:'DroidSans';font-size:11.96pt;color:#000000">We use</span><span style="font-family:'F44';font-size:11.96pt;color:#000000"> volatile</span><span style="font-family:'DroidSans';font-size:11.96pt;color:#000000"> that is naturally coherent</span>
<span style="font-family:'DroidSans';font-size:11.96pt;color:#000000">We use weaker forms of</span><span style="font-family:'F44';font-size:11.96pt;color:#000000"> VarHandles</span><span style="font-family:'DroidSans';font-size:11.96pt;color:#000000"> that are coherent</span>
<span style="font-family:'DroidSans';font-size:11.96pt;color:#000000">We use properly synchronized (non-racy) reads</span>

<span style="font-family:'DroidSans';font-size:5.98pt;color:#d9d9d9">Slide 32/86. «Java Memory Model Unlearning Experience», Aleksey Shipilёv, 2018, D:20180511133537+02’00’</span>

---

## Page 56

<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">Coherence: JMM and Ordering Modes</span>

<span style="font-family:'F19';font-size:14.35pt;color:#000000">Java 8</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">Java 9</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">–</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">–</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">N</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">N</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">plain</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">VH Plain</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">Y</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">N</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">–</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">VH Opaque</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">Y</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">Y</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">volatile</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">VH SeqCst</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">Y</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">Y</span>

<span style="font-family:'DroidSans';font-size:5.98pt;color:#d9d9d9">Slide 33/86. «Java Memory Model Unlearning Experience», Aleksey Shipilёv, 2018, D:20180511133537+02’00’</span>

<span style="font-family:'F19';font-size:14.35pt;color:#000000">Coherence</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">Definite</span>

---

## Page 57

Causality

<!-- 图源: ./geecon-May2018-jmm_assets/images/p057_scan.jpg -->


<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">Causality</span>

---

## Page 58

<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">Causality: SW – Synchronizes-With Order</span>

<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">When one SA «sees» the value of another SA,</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">they are said to be in «synchronizes-with» ( SW ) relation</span>

<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">SW is a partial order</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">SW connects the operations that «see» each other</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Acts like the «bridge» between the threads</span>

<span style="font-family:'DroidSans';font-size:5.98pt;color:#d9d9d9">Slide 35/86. «Java Memory Model Unlearning Experience», Aleksey Shipilёv, 2018, D:20180511133537+02’00’</span>

---

## Page 59

<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">Causality: HB – Happens-Before Order</span>

<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">HB is a transitive closure</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">over the union of PO and SW</span>
<span style="font-family:'DroidSans-Bold';font-size:14.35pt;font-weight:bold;color:#000000">HB</span><span style="font-family:'DroidSans';font-size:14.35pt;color:#000000"> is a partial order</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">(Translation: not everything is connected)</span>
<span style="font-family:'DroidSans-Bold';font-size:14.35pt;font-weight:bold;color:#000000">HB consistency</span><span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">: reads observe either:</span>

<span style="font-family:'DroidSans';font-size:7.97pt;color:#000000">hb</span>
<span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000">−−→</span><span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">, or</span>

<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">the last write in</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">any other write, not ordered by</span>

<span style="font-family:'DroidSans';font-size:5.98pt;color:#d9d9d9">Slide 36/86. «Java Memory Model Unlearning Experience», Aleksey Shipilёv, 2018, D:20180511133537+02’00’</span>

<span style="font-family:'DroidSans';font-size:7.97pt;color:#000000">hb</span>
<span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000">−−→</span>

---

## Page 60

<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">Causality: Example 3.1</span>

<span style="font-family:'F19';font-size:14.35pt;color:#000000">int</span><span style="font-family:'F19';font-size:14.35pt;color:#0000ff"> x</span><span style="font-family:'F19';font-size:14.35pt;color:#000000">;</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">volatile int</span><span style="font-family:'F19';font-size:14.35pt;color:#ff0000"> y</span><span style="font-family:'F19';font-size:14.35pt;color:#000000">;</span>
<span style="font-family:'F19';font-size:14.35pt;color:#0000ff">x</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> = 1;</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">r1 =</span><span style="font-family:'F19';font-size:14.35pt;color:#ff0000"> y</span><span style="font-family:'F19';font-size:14.35pt;color:#000000">;</span>
<span style="font-family:'F19';font-size:14.35pt;color:#ff0000">y</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> = 1;</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">r2 =</span><span style="font-family:'F19';font-size:14.35pt;color:#0000ff"> x</span><span style="font-family:'F19';font-size:14.35pt;color:#000000">;</span>

<span style="font-family:'DroidSans';font-size:5.98pt;color:#d9d9d9">Slide 37/86. «Java Memory Model Unlearning Experience», Aleksey Shipilёv, 2018, D:20180511133537+02’00’</span>

---

## Page 61

<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">Causality: Example 3.1</span>

<span style="font-family:'F19';font-size:14.35pt;color:#000000">int</span><span style="font-family:'F19';font-size:14.35pt;color:#0000ff"> x</span><span style="font-family:'F19';font-size:14.35pt;color:#000000">;</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">volatile int</span><span style="font-family:'F19';font-size:14.35pt;color:#ff0000"> y</span><span style="font-family:'F19';font-size:14.35pt;color:#000000">;</span>
<span style="font-family:'F19';font-size:14.35pt;color:#0000ff">x</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> = 1;</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">r1 =</span><span style="font-family:'F19';font-size:14.35pt;color:#ff0000"> y</span><span style="font-family:'F19';font-size:14.35pt;color:#000000">;</span>
<span style="font-family:'F19';font-size:14.35pt;color:#ff0000">y</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> = 1;</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">r2 =</span><span style="font-family:'F19';font-size:14.35pt;color:#0000ff"> x</span><span style="font-family:'F19';font-size:14.35pt;color:#000000">;</span>

<span style="font-family:'DroidSans';font-size:7.97pt;color:#000000">po</span>
<span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000">−−→</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑤</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑦,</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000"> 1)</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000"> ... 𝑟</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑦</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">) :</span><span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000"> *</span>

<span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑤</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑥,</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000"> 1)</span>

<span style="font-family:'DroidSans';font-size:5.98pt;color:#d9d9d9">Slide 37/86. «Java Memory Model Unlearning Experience», Aleksey Shipilёv, 2018, D:20180511133537+02’00’</span>

<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">We are dealing with this class of executions:</span>

<span style="font-family:'DroidSans';font-size:7.97pt;color:#000000">po</span>
<span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000">−−→</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑟</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑥</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">) :</span><span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000"> *</span>

---

## Page 62

<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">Causality: Example 3.1</span>

<span style="font-family:'F19';font-size:14.35pt;color:#000000">int</span><span style="font-family:'F19';font-size:14.35pt;color:#0000ff"> x</span><span style="font-family:'F19';font-size:14.35pt;color:#000000">;</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">volatile int</span><span style="font-family:'F19';font-size:14.35pt;color:#ff0000"> y</span><span style="font-family:'F19';font-size:14.35pt;color:#000000">;</span>
<span style="font-family:'F19';font-size:14.35pt;color:#0000ff">x</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> = 1;</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">r1 =</span><span style="font-family:'F19';font-size:14.35pt;color:#ff0000"> y</span><span style="font-family:'F19';font-size:14.35pt;color:#000000">;</span>
<span style="font-family:'F19';font-size:14.35pt;color:#ff0000">y</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> = 1;</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">r2 =</span><span style="font-family:'F19';font-size:14.35pt;color:#0000ff"> x</span><span style="font-family:'F19';font-size:14.35pt;color:#000000">;</span>

<span style="font-family:'DroidSans';font-size:7.97pt;color:#000000">hb</span>
<span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000">−−→</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑤</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑦,</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000"> 1)</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000"> ... 𝑟</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑦</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">) : 0</span>

<span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑤</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑥,</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000"> 1)</span>

<span style="font-family:'DroidSans';font-size:7.97pt;color:#000000">hb</span>
<span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000">−−→</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑤</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑦,</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000"> 1)</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000"> ... 𝑟</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑦</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">) : 0</span>

<span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑤</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑥,</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000"> 1)</span>

<span style="font-family:'DroidSans';font-size:5.98pt;color:#d9d9d9">Slide 37/86. «Java Memory Model Unlearning Experience», Aleksey Shipilёv, 2018, D:20180511133537+02’00’</span>

<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Racy subclass:</span>

<span style="font-family:'DroidSans';font-size:7.97pt;color:#000000">hb</span>
<span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000">−−→</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑟</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑥</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">) : 0</span>
<span style="font-family:'DroidSans';font-size:7.97pt;color:#000000">hb</span>
<span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000">−−→</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑟</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑥</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">) : 1</span>

---

## Page 63

<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">Causality: Example 3.1</span>

<span style="font-family:'F19';font-size:14.35pt;color:#000000">int</span><span style="font-family:'F19';font-size:14.35pt;color:#0000ff"> x</span><span style="font-family:'F19';font-size:14.35pt;color:#000000">;</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">volatile int</span><span style="font-family:'F19';font-size:14.35pt;color:#ff0000"> y</span><span style="font-family:'F19';font-size:14.35pt;color:#000000">;</span>
<span style="font-family:'F19';font-size:14.35pt;color:#0000ff">x</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> = 1;</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">r1 =</span><span style="font-family:'F19';font-size:14.35pt;color:#ff0000"> y</span><span style="font-family:'F19';font-size:14.35pt;color:#000000">;</span>
<span style="font-family:'F19';font-size:14.35pt;color:#ff0000">y</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> = 1;</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">r2 =</span><span style="font-family:'F19';font-size:14.35pt;color:#0000ff"> x</span><span style="font-family:'F19';font-size:14.35pt;color:#000000">;</span>

<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Non-racy subclass:</span>
<span style="font-family:'DroidSans';font-size:7.97pt;color:#000000">hb</span>
<span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000">−−→</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑟</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑦</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">) : 1</span>

<span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑤</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑥,</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000"> 1)</span>
<span style="font-family:'DroidSans';font-size:7.97pt;color:#000000">hb</span>
<span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000">−−→</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑤</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑦,</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000"> 1)</span>
<span style="font-family:'DroidSans';font-size:7.97pt;color:#000000">hb</span>
<span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000">−−→</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑟</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑥</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">) : 1</span>

<span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑤</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑥,</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000"> 1)</span>
<span style="font-family:'DroidSans';font-size:7.97pt;color:#000000">hb</span>
<span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000">−−→</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑤</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑦,</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000"> 1)</span>
<span style="font-family:'DroidSans';font-size:7.97pt;color:#000000">hb</span>
<span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000">−−→</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑟</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑦</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">) : 1</span>
<span style="font-family:'DroidSans';font-size:7.97pt;color:#000000">hb</span>
<span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000">−−→</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑟</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑥</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">) : 0</span>

<span style="font-family:'DroidSans';font-size:5.98pt;color:#d9d9d9">Slide 37/86. «Java Memory Model Unlearning Experience», Aleksey Shipilёv, 2018, D:20180511133537+02’00’</span>

---

## Page 64

<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">Causality: Look Closer, #1</span>

<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Happens-before is deﬁned over</span><span style="font-family:'DroidSans-Slant_213';font-size:14.35pt;font-style:italic;color:#000000"> actions</span><span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">,</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">not over statements: notice no HB between</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> volatile</span><span style="font-family:'DroidSans';font-size:14.35pt;color:#000000"> ops!</span>

<span style="font-family:'DroidSans';font-size:7.97pt;color:#000000">hb</span>
<span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000">−−→</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑤</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑦,</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000"> 1)</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000"> ... 𝑟</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑦</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">) : 0</span>
<span style="font-family:'CMMI12';font-size:11.96pt;font-style:italic;color:#000000">𝑤</span><span style="font-family:'CMR12';font-size:11.96pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:11.96pt;font-style:italic;color:#000000">𝑥,</span><span style="font-family:'CMR12';font-size:11.96pt;color:#000000"> 1)</span>

<span style="font-family:'DroidSans-Slant_213';font-size:11.96pt;font-style:italic;color:#000000">not required to see this</span>

<span style="font-family:'DroidSans';font-size:5.98pt;color:#d9d9d9">Slide 38/86. «Java Memory Model Unlearning Experience», Aleksey Shipilёv, 2018, D:20180511133537+02’00’</span>

<span style="font-family:'DroidSans';font-size:7.97pt;color:#000000">hb</span>
<span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000">−−→</span> <span style="font-family:'CMMI12';font-size:11.96pt;font-style:italic;color:#000000">𝑟</span><span style="font-family:'CMR12';font-size:11.96pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:11.96pt;font-style:italic;color:#000000">𝑥</span><span style="font-family:'CMR12';font-size:11.96pt;color:#000000">) : 0</span>

---

## Page 65

<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">Causality: Look Closer, #2</span>

<span style="font-family:'DroidSans';font-size:7.97pt;color:#000000">hb</span>
<span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000">−−→</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑤</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑦,</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000"> 1)</span>
<span style="font-family:'CMMI12';font-size:11.96pt;font-style:italic;color:#000000">𝑤</span><span style="font-family:'CMR12';font-size:11.96pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:11.96pt;font-style:italic;color:#000000">𝑥,</span><span style="font-family:'CMR12';font-size:11.96pt;color:#000000"> 1)</span>

<span style="font-family:'DroidSans-Slant_213';font-size:11.96pt;font-style:italic;color:#000000">should have seen this!</span>

<span style="font-family:'DroidSans-Bold';font-size:14.35pt;font-weight:bold;color:#000000">Causality:</span><span style="font-family:'DroidSans';font-size:14.35pt;color:#000000"> Observing the</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> volatile</span><span style="font-family:'DroidSans';font-size:14.35pt;color:#000000"> store causes observing</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">everything stored before it</span>
<span style="font-family:'DroidSans';font-size:5.98pt;color:#d9d9d9">Slide 39/86. «Java Memory Model Unlearning Experience», Aleksey Shipilёv, 2018, D:20180511133537+02’00’</span>

<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">This violates HB consistency:</span>

<span style="font-family:'DroidSans';font-size:7.97pt;color:#000000">hb</span>
<span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000">−−→</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑟</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑦</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">) : 1</span>

<span style="font-family:'DroidSans';font-size:7.97pt;color:#000000">hb</span>
<span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000">−−→</span> <span style="font-family:'CMMI12';font-size:11.96pt;font-style:italic;color:#000000">𝑟</span><span style="font-family:'CMR12';font-size:11.96pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:11.96pt;font-style:italic;color:#000000">𝑥</span><span style="font-family:'CMR12';font-size:11.96pt;color:#000000">) : 0</span>

---

## Page 66

<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">Causality: Example 3.2</span>

<span style="font-family:'F19';font-size:14.35pt;color:#000000">int</span><span style="font-family:'F19';font-size:14.35pt;color:#0000ff"> x</span><span style="font-family:'F19';font-size:14.35pt;color:#000000">;</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">volatile int</span><span style="font-family:'F19';font-size:14.35pt;color:#ff0000"> y</span><span style="font-family:'F19';font-size:14.35pt;color:#000000">;</span>
<span style="font-family:'F19';font-size:14.35pt;color:#ff0000">y</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> = 1;</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">r1 =</span><span style="font-family:'F19';font-size:14.35pt;color:#0000ff"> x</span><span style="font-family:'F19';font-size:14.35pt;color:#000000">;</span>
<span style="font-family:'F19';font-size:14.35pt;color:#0000ff">x</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> = 1;</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">r2 =</span><span style="font-family:'F19';font-size:14.35pt;color:#ff0000"> y</span><span style="font-family:'F19';font-size:14.35pt;color:#000000">;</span>

<span style="font-family:'DroidSans';font-size:5.98pt;color:#d9d9d9">Slide 40/86. «Java Memory Model Unlearning Experience», Aleksey Shipilёv, 2018, D:20180511133537+02’00’</span>

<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Notice the order</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">is different</span>

---

## Page 67

<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">Causality: Example 3.2</span>

<span style="font-family:'F19';font-size:14.35pt;color:#000000">int</span><span style="font-family:'F19';font-size:14.35pt;color:#0000ff"> x</span><span style="font-family:'F19';font-size:14.35pt;color:#000000">;</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">volatile int</span><span style="font-family:'F19';font-size:14.35pt;color:#ff0000"> y</span><span style="font-family:'F19';font-size:14.35pt;color:#000000">;</span>
<span style="font-family:'F19';font-size:14.35pt;color:#ff0000">y</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> = 1;</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">r1 =</span><span style="font-family:'F19';font-size:14.35pt;color:#0000ff"> x</span><span style="font-family:'F19';font-size:14.35pt;color:#000000">;</span>
<span style="font-family:'F19';font-size:14.35pt;color:#0000ff">x</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> = 1;</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">r2 =</span><span style="font-family:'F19';font-size:14.35pt;color:#ff0000"> y</span><span style="font-family:'F19';font-size:14.35pt;color:#000000">;</span>

<span style="font-family:'DroidSans';font-size:7.97pt;color:#000000">hb</span>
<span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000">−−→</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑤</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑥,</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000"> 1)</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000"> ... 𝑟</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑥</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">) : 1</span>

<span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑤</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑦,</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000"> 1)</span>

<span style="font-family:'DroidSans';font-size:5.98pt;color:#d9d9d9">Slide 40/86. «Java Memory Model Unlearning Experience», Aleksey Shipilёv, 2018, D:20180511133537+02’00’</span>

<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Notice the order</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">is different</span>

<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Hey, look how</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000"> (1</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">,</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000"> 0)</span><span style="font-family:'DroidSans';font-size:14.35pt;color:#000000"> is allowed:</span>

<span style="font-family:'DroidSans';font-size:7.97pt;color:#000000">hb</span>
<span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000">−−→</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑟</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑦</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">) : 0</span>

---

## Page 68

<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">Causality: Example 3.2</span>

<span style="font-family:'F19';font-size:14.35pt;color:#000000">int</span><span style="font-family:'F19';font-size:14.35pt;color:#0000ff"> x</span><span style="font-family:'F19';font-size:14.35pt;color:#000000">;</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">volatile int</span><span style="font-family:'F19';font-size:14.35pt;color:#ff0000"> y</span><span style="font-family:'F19';font-size:14.35pt;color:#000000">;</span>
<span style="font-family:'F19';font-size:14.35pt;color:#ff0000">y</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> = 1;</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">r1 =</span><span style="font-family:'F19';font-size:14.35pt;color:#0000ff"> x</span><span style="font-family:'F19';font-size:14.35pt;color:#000000">;</span>
<span style="font-family:'F19';font-size:14.35pt;color:#0000ff">x</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> = 1;</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">r2 =</span><span style="font-family:'F19';font-size:14.35pt;color:#ff0000"> y</span><span style="font-family:'F19';font-size:14.35pt;color:#000000">;</span>

<span style="font-family:'DroidSans';font-size:7.97pt;color:#000000">hb</span>
<span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000">−−→</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑤</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑥,</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000"> 1)</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000"> ... 𝑟</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑥</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">) : 1</span>

<span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑤</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑦,</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000"> 1)</span>

<span style="font-family:'DroidSans';font-size:5.98pt;color:#d9d9d9">Slide 40/86. «Java Memory Model Unlearning Experience», Aleksey Shipilёv, 2018, D:20180511133537+02’00’</span>

<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Notice the order</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">is different</span>

<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Hey, look how</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000"> (1</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">,</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000"> 0)</span><span style="font-family:'DroidSans';font-size:14.35pt;color:#000000"> is allowed:</span>

<span style="font-family:'DroidSans';font-size:7.97pt;color:#000000">hb</span>
<span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000">−−→</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑟</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑦</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">) : 0</span>

<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Look: irrelevant that</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> y</span><span style="font-family:'DroidSans';font-size:14.35pt;color:#000000"> is</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> volatile</span><span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">!</span>

---

## Page 69

<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">Causality: Safe Publication</span>

<span style="font-family:'F44';font-size:11.96pt;color:#008000">volatile</span><span style="font-family:'F44';font-size:11.96pt;color:#b00040"> int</span><span style="font-family:'F44';font-size:11.96pt;color:#000000"> v</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">;</span>

<span style="font-family:'F44';font-size:11.96pt;color:#000000">x</span><span style="font-family:'F44';font-size:11.96pt;color:#666666"> = ...;</span>
<span style="font-family:'F44';font-size:11.96pt;color:#000000">y</span><span style="font-family:'F44';font-size:11.96pt;color:#666666"> = ...;</span>
<span style="font-family:'F44';font-size:11.96pt;color:#000000">z</span><span style="font-family:'F44';font-size:11.96pt;color:#666666"> = ...;</span>
<span style="font-family:'F44';font-size:11.96pt;color:#000000">v</span><span style="font-family:'F44';font-size:11.96pt;color:#666666"> = 1;</span>
<span style="font-family:'CMMI12';font-size:11.96pt;font-style:italic;color:#000000">𝑟𝑒𝑙𝑒𝑎𝑠𝑒</span>

<span style="font-family:'F44';font-size:11.96pt;color:#000000">send</span>

<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">As if «commits to memory», but only for acq/rel pair</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">release</span><span style="font-family:'DroidSans';font-size:14.35pt;color:#000000"> «commits»,</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> acquire</span><span style="font-family:'DroidSans';font-size:14.35pt;color:#000000"> gets the committed</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">acquire</span><span style="font-family:'DroidSans';font-size:14.35pt;color:#000000"> has to see</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> release</span><span style="font-family:'DroidSans';font-size:14.35pt;color:#000000"> witness!</span>
<span style="font-family:'DroidSans';font-size:5.98pt;color:#d9d9d9">Slide 41/86. «Java Memory Model Unlearning Experience», Aleksey Shipilёv, 2018, D:20180511133537+02’00’</span>

<span style="font-family:'CMMI12';font-size:11.96pt;font-style:italic;color:#000000">𝑎𝑐𝑞𝑢𝑖𝑟𝑒</span>
<span style="font-family:'F44';font-size:11.96pt;color:#008000">if</span><span style="font-family:'F44';font-size:11.96pt;color:#666666"> (</span><span style="font-family:'F44';font-size:11.96pt;color:#000000">v</span><span style="font-family:'F44';font-size:11.96pt;color:#666666"> == 1) {</span>
<span style="font-family:'F44';font-size:11.96pt;color:#b00040">int</span><span style="font-family:'F44';font-size:11.96pt;color:#000000"> lx</span><span style="font-family:'F44';font-size:11.96pt;color:#666666"> =</span><span style="font-family:'F44';font-size:11.96pt;color:#000000"> x</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">;</span>
<span style="font-family:'F44';font-size:11.96pt;color:#b00040">int</span><span style="font-family:'F44';font-size:11.96pt;color:#000000"> ly</span><span style="font-family:'F44';font-size:11.96pt;color:#666666"> =</span><span style="font-family:'F44';font-size:11.96pt;color:#000000"> y</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">;</span>
<span style="font-family:'F44';font-size:11.96pt;color:#b00040">int</span><span style="font-family:'F44';font-size:11.96pt;color:#000000"> lz</span><span style="font-family:'F44';font-size:11.96pt;color:#666666"> =</span><span style="font-family:'F44';font-size:11.96pt;color:#000000"> z</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">;</span>
<span style="font-family:'F44';font-size:11.96pt;color:#666666">}</span>

---

## Page 70

<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">Causality: Takeaway #5</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">1. Safe publication is the major (and simple) rule</span>

<span style="font-family:'DroidSans';font-size:11.96pt;color:#000000">Identify your</span><span style="font-family:'DroidSans-Bold';font-size:11.96pt;font-weight:bold;color:#000000"> acquires</span><span style="font-family:'DroidSans';font-size:11.96pt;color:#000000"> and</span><span style="font-family:'DroidSans-Bold';font-size:11.96pt;font-weight:bold;color:#000000"> releases</span>
<span style="font-family:'DroidSans';font-size:11.96pt;color:#000000">Check that</span><span style="font-family:'DroidSans-Bold';font-size:11.96pt;font-weight:bold;color:#000000"> acquires</span><span style="font-family:'DroidSans';font-size:11.96pt;color:#000000">/</span><span style="font-family:'DroidSans-Bold';font-size:11.96pt;font-weight:bold;color:#000000">releases</span><span style="font-family:'DroidSans';font-size:11.96pt;color:#000000"> are on all paths</span>
<span style="font-family:'DroidSans';font-size:11.96pt;color:#000000">Learn this rule! Then learn it again!</span>

<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">2. The whole thing does not require JMM reasoning</span>

<span style="font-family:'DroidSans';font-size:11.96pt;color:#000000">Hardly anyone applies «happens-before» correctly</span>
<span style="font-family:'DroidSans';font-size:11.96pt;color:#000000">Hardly anyone can do it reliably</span>
<span style="font-family:'DroidSans';font-size:11.96pt;color:#000000">It is very easy to miss the racy access</span>

<span style="font-family:'DroidSans';font-size:5.98pt;color:#d9d9d9">Slide 42/86. «Java Memory Model Unlearning Experience», Aleksey Shipilёv, 2018, D:20180511133537+02’00’</span>

---

## Page 71

<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">Causality: JMM and Ordering Modes</span>

<span style="font-family:'F19';font-size:14.35pt;color:#000000">Java 8</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">Java 9</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">–</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">–</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">N</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">N</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">N</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">plain</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">VH Plain</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">Y</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">N</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">N</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">–</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">VH Opaque</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">Y</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">Y</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">N</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">–</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">VH Acq/Rel</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">Y</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">Y</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">Y</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">volatile</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">VH SeqCst</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">Y</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">Y</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">Y</span>

<span style="font-family:'DroidSans';font-size:5.98pt;color:#d9d9d9">Slide 43/86. «Java Memory Model Unlearning Experience», Aleksey Shipilёv, 2018, D:20180511133537+02’00’</span>

<span style="font-family:'F19';font-size:14.35pt;color:#000000">Coherence</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">Causality</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">Definite</span>

---

## Page 72

Consensus

<!-- 图源: ./geecon-May2018-jmm_assets/images/p072_scan.jpg -->


<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">Consensus</span>

---

## Page 73

<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">Consensus: Example 4.1</span>

<span style="font-family:'F19';font-size:14.35pt;color:#000000">volatile int</span><span style="font-family:'F19';font-size:14.35pt;color:#ff0000"> x</span><span style="font-family:'F19';font-size:14.35pt;color:#000000">,</span><span style="font-family:'F19';font-size:14.35pt;color:#0000ff"> y</span><span style="font-family:'F19';font-size:14.35pt;color:#000000">;</span>
<span style="font-family:'F19';font-size:14.35pt;color:#ff0000">x</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> = 1;</span>
<span style="font-family:'F19';font-size:14.35pt;color:#0000ff">y</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> = 1;</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">int r1 =</span><span style="font-family:'F19';font-size:14.35pt;color:#0000ff"> y</span><span style="font-family:'F19';font-size:14.35pt;color:#000000">;</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">int r3 =</span><span style="font-family:'F19';font-size:14.35pt;color:#ff0000"> x</span><span style="font-family:'F19';font-size:14.35pt;color:#000000">;</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">int r2 =</span><span style="font-family:'F19';font-size:14.35pt;color:#ff0000"> x</span><span style="font-family:'F19';font-size:14.35pt;color:#000000">;</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">int r4 =</span><span style="font-family:'F19';font-size:14.35pt;color:#0000ff"> y</span><span style="font-family:'F19';font-size:14.35pt;color:#000000">;</span>

<span style="font-family:'DroidSans';font-size:7.97pt;color:#000000">hb</span>
<span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000">−−→</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑟</span><span style="font-family:'CMR10';font-size:9.96pt;color:#000000">1</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑦</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">) : 1</span>

<span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑤</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑦,</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000"> 1)</span>

<span style="font-family:'DroidSans';font-size:7.97pt;color:#000000">hb</span>
<span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000">−−→</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑟</span><span style="font-family:'CMR10';font-size:9.96pt;color:#000000">2</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑥</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">) : 1</span>

<span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑤</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑥,</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000"> 1)</span>

<span style="font-family:'DroidSans';font-size:5.98pt;color:#d9d9d9">Slide 45/86. «Java Memory Model Unlearning Experience», Aleksey Shipilёv, 2018, D:20180511133537+02’00’</span>

<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">HB alone allows seeing</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000"> (1</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">,</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000"> 0</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">,</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000"> 1</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">,</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000"> 0)</span><span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">:</span>

<span style="font-family:'DroidSans';font-size:7.97pt;color:#000000">hb</span>
<span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000">−−→</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑟</span><span style="font-family:'CMR10';font-size:9.96pt;color:#000000">3</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑥</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">) : 0</span>
<span style="font-family:'DroidSans';font-size:7.97pt;color:#000000">hb</span>
<span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000">−−→</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑟</span><span style="font-family:'CMR10';font-size:9.96pt;color:#000000">4</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑦</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">) : 0</span>

---

## Page 74

<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">Consensus: SC</span>

<span style="font-family:'DroidSans-Bold';font-size:14.35pt;font-weight:bold;color:#000000">Sequential Consistency (SC):</span><span style="font-family:'DroidSans-Slant_213';font-size:14.35pt;font-style:italic;color:#000000"> (def.)</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">«...the result of any execution is the same as if the</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">operations of all the processors were executed</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">in some sequential order, and the operations of</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">each individual processor appear in this sequence</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">in the order speciﬁed by its program»</span>

<span style="font-family:'DroidSans';font-size:5.98pt;color:#d9d9d9">Slide 46/86. «Java Memory Model Unlearning Experience», Aleksey Shipilёv, 2018, D:20180511133537+02’00’</span>

---

## Page 75

<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">Consensus: SO – Synchronization Order</span>

<span style="font-family:'DroidSans-Bold';font-size:14.35pt;font-weight:bold;color:#000000">SO</span><span style="font-family:'DroidSans';font-size:14.35pt;color:#000000"> is a total order («All SA actions relate to each other»)</span>
<span style="font-family:'DroidSans-Bold';font-size:14.35pt;font-weight:bold;color:#000000">SO - PO consistency</span><span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">:</span>
<span style="font-family:'DroidSans-Bold';font-size:14.35pt;font-weight:bold;color:#000000">SO consistency</span><span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">: reads see only the latest write in</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Just what Sequential Consistency wants!</span>
<span style="font-family:'DroidSans';font-size:5.98pt;color:#d9d9d9">Slide 47/86. «Java Memory Model Unlearning Experience», Aleksey Shipilёv, 2018, D:20180511133537+02’00’</span>

<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">SO covers all</span><span style="font-family:'DroidSans-Slant_213';font-size:14.35pt;font-style:italic;color:#000000"> synchronization actions</span><span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">:</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">volatile read/write, lock/unlock, etc.</span>

<span style="font-family:'DroidSans';font-size:7.97pt;color:#000000">po</span>
<span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000">−−→</span><span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">agree</span>

<span style="font-family:'DroidSans';font-size:7.97pt;color:#000000">so</span>
<span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000">−−→</span><span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">and</span>

<span style="font-family:'DroidSans';font-size:7.97pt;color:#000000">so</span>
<span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000">−−→</span>

---

## Page 76

<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">Consensus: Takeaway #6</span>

<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">1. SO</span><span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000"> ≈</span><span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Sequential Consistency</span>

<span style="font-family:'DroidSans';font-size:11.96pt;color:#000000">Want SC? You have to go full-blown</span><span style="font-family:'F44';font-size:11.96pt;color:#000000"> volatile</span>
<span style="font-family:'DroidSans';font-size:11.96pt;color:#000000">Seed enough</span><span style="font-family:'F44';font-size:11.96pt;color:#000000"> volatiles</span><span style="font-family:'DroidSans';font-size:11.96pt;color:#000000"> around your program, and it</span>
<span style="font-family:'DroidSans';font-size:11.96pt;color:#000000">eventually becomes data-race-free! /s</span>

<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">2. Sequential Consistency is not always needed</span>

<span style="font-family:'DroidSans';font-size:11.96pt;color:#000000">Extreme costs to get it in distributed systems</span>
<span style="font-family:'DroidSans';font-size:11.96pt;color:#000000">Most examples so far were ﬁne with just Release/Acquire!</span>

<span style="font-family:'DroidSans';font-size:5.98pt;color:#d9d9d9">Slide 48/86. «Java Memory Model Unlearning Experience», Aleksey Shipilёv, 2018, D:20180511133537+02’00’</span>

---

## Page 77

<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">Consensus: JMM and Ordering Modes</span>

<span style="font-family:'F19';font-size:14.35pt;color:#000000">Java 8</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">Java 9</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">–</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">–</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">N</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">N</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">N</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">N</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">plain</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">VH Plain</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">Y</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">N</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">N</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">N</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">–</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">VH Opaque</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">Y</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">Y</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">N</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">N</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">–</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">VH Acq/Rel</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">Y</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">Y</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">Y</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">N</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">volatile</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">VH SeqCst</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">Y</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">Y</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">Y</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">Y</span>

<span style="font-family:'DroidSans';font-size:5.98pt;color:#d9d9d9">Slide 49/86. «Java Memory Model Unlearning Experience», Aleksey Shipilёv, 2018, D:20180511133537+02’00’</span>

<span style="font-family:'F19';font-size:14.35pt;color:#000000">Coherence</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">Causality</span>

<span style="font-family:'F19';font-size:14.35pt;color:#000000">Consensus</span>

<span style="font-family:'F19';font-size:14.35pt;color:#000000">Definite</span>

---

## Page 78

<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">Finals: Example 5.1</span>

<span style="font-family:'F19';font-size:14.35pt;color:#000000">class M { final int x = 42; }</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">M</span><span style="font-family:'F19';font-size:14.35pt;color:#ff0000"> m</span><span style="font-family:'F19';font-size:14.35pt;color:#000000">;</span>
<span style="font-family:'F19';font-size:14.35pt;color:#ff0000">m</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> = new M()</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">M</span><span style="font-family:'F19';font-size:14.35pt;color:#0000ff"> lm</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> =</span><span style="font-family:'F19';font-size:14.35pt;color:#ff0000"> m</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">if (</span><span style="font-family:'F19';font-size:14.35pt;color:#0000ff">lm</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> != null)</span>

<span style="font-family:'DroidSans';font-size:5.98pt;color:#d9d9d9">Slide 50/86. «Java Memory Model Unlearning Experience», Aleksey Shipilёv, 2018, D:20180511133537+02’00’</span>

<span style="font-family:'F19';font-size:14.35pt;color:#000000">r1 =</span><span style="font-family:'F19';font-size:14.35pt;color:#0000ff"> lm</span><span style="font-family:'F19';font-size:14.35pt;color:#000000">.x</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">else</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">r1 = 1</span>

---

## Page 79

<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">Finals: Example 5.1</span>

<span style="font-family:'F19';font-size:14.35pt;color:#000000">class M { final int x = 42; }</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">M</span><span style="font-family:'F19';font-size:14.35pt;color:#ff0000"> m</span><span style="font-family:'F19';font-size:14.35pt;color:#000000">;</span>
<span style="font-family:'F19';font-size:14.35pt;color:#ff0000">m</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> = new M()</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">M</span><span style="font-family:'F19';font-size:14.35pt;color:#0000ff"> lm</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> =</span><span style="font-family:'F19';font-size:14.35pt;color:#ff0000"> m</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">if (</span><span style="font-family:'F19';font-size:14.35pt;color:#0000ff">lm</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> != null)</span>

<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">JMM guarantees seeing the value of</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> final</span><span style="font-family:'DroidSans';font-size:14.35pt;color:#000000"> ﬁeld here:</span>
<span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑟</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">1</span><span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000"> ∈{</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">1</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">,</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000"> 42</span><span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000">}</span>
<span style="font-family:'DroidSans';font-size:5.98pt;color:#d9d9d9">Slide 50/86. «Java Memory Model Unlearning Experience», Aleksey Shipilёv, 2018, D:20180511133537+02’00’</span>

<span style="font-family:'F19';font-size:14.35pt;color:#000000">r1 =</span><span style="font-family:'F19';font-size:14.35pt;color:#0000ff"> lm</span><span style="font-family:'F19';font-size:14.35pt;color:#000000">.x</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">else</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">r1 = 1</span>

---

## Page 80

<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">Finals: Example 5.1</span>

<span style="font-family:'F19';font-size:14.35pt;color:#000000">class M { final int x = 42; }</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">M</span><span style="font-family:'F19';font-size:14.35pt;color:#ff0000"> m</span><span style="font-family:'F19';font-size:14.35pt;color:#000000">;</span>
<span style="font-family:'F19';font-size:14.35pt;color:#ff0000">m</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> = new M()</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">M</span><span style="font-family:'F19';font-size:14.35pt;color:#0000ff"> lm</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> =</span><span style="font-family:'F19';font-size:14.35pt;color:#ff0000"> m</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">if (</span><span style="font-family:'F19';font-size:14.35pt;color:#0000ff">lm</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> != null)</span>

<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Special rule, if</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> x</span><span style="font-family:'DroidSans';font-size:14.35pt;color:#000000"> is a</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> final</span><span style="font-family:'DroidSans';font-size:14.35pt;color:#000000"> ﬁeld:</span>
<span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑤</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑥,</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000"> 42)</span>

<span style="font-family:'DroidSans';font-size:5.98pt;color:#d9d9d9">Slide 50/86. «Java Memory Model Unlearning Experience», Aleksey Shipilёv, 2018, D:20180511133537+02’00’</span>

<span style="font-family:'F19';font-size:14.35pt;color:#000000">r1 =</span><span style="font-family:'F19';font-size:14.35pt;color:#0000ff"> lm</span><span style="font-family:'F19';font-size:14.35pt;color:#000000">.x</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">else</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">r1 = 1</span>

<span style="font-family:'DroidSans';font-size:7.97pt;color:#000000">hb</span>
<span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000">−−→</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑟</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑥</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">) : 42</span>

---

## Page 81

<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">Finals: Example 5.2</span>

<span style="font-family:'F19';font-size:14.35pt;color:#000000">class M {</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000"> 𝑣𝑜𝑙𝑎𝑡𝑖𝑙𝑒</span> <span style="font-family:'F19';font-size:14.35pt;color:#000000">int x = 42; }</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">M</span><span style="font-family:'F19';font-size:14.35pt;color:#ff0000"> m</span><span style="font-family:'F19';font-size:14.35pt;color:#000000">;</span>
<span style="font-family:'F19';font-size:14.35pt;color:#ff0000">m</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> = new M()</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">M</span><span style="font-family:'F19';font-size:14.35pt;color:#0000ff"> lm</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> =</span><span style="font-family:'F19';font-size:14.35pt;color:#ff0000"> m</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">if (</span><span style="font-family:'F19';font-size:14.35pt;color:#0000ff">lm</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> != null)</span>

<span style="font-family:'F19';font-size:14.35pt;color:#000000">r1 =</span><span style="font-family:'F19';font-size:14.35pt;color:#0000ff"> lm</span><span style="font-family:'F19';font-size:14.35pt;color:#000000">.x</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">else</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">r1 = 1</span>

<span style="font-family:'DroidSans';font-size:5.98pt;color:#d9d9d9">Slide 51/86. «Java Memory Model Unlearning Experience», Aleksey Shipilёv, 2018, D:20180511133537+02’00’</span>

---

## Page 82

<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">Finals: Example 5.2</span>

<span style="font-family:'F19';font-size:14.35pt;color:#000000">class M {</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000"> 𝑣𝑜𝑙𝑎𝑡𝑖𝑙𝑒</span> <span style="font-family:'F19';font-size:14.35pt;color:#000000">int x = 42; }</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">M</span><span style="font-family:'F19';font-size:14.35pt;color:#ff0000"> m</span><span style="font-family:'F19';font-size:14.35pt;color:#000000">;</span>
<span style="font-family:'F19';font-size:14.35pt;color:#ff0000">m</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> = new M()</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">M</span><span style="font-family:'F19';font-size:14.35pt;color:#0000ff"> lm</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> =</span><span style="font-family:'F19';font-size:14.35pt;color:#ff0000"> m</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">if (</span><span style="font-family:'F19';font-size:14.35pt;color:#0000ff">lm</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> != null)</span>

<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">JMM allows</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000"> (0)</span><span style="font-family:'DroidSans';font-size:14.35pt;color:#000000"> here:</span>
<span style="font-family:'DroidSans';font-size:7.97pt;color:#000000">hb</span>
<span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000">−−→</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑤</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑐𝑚, 𝑚</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">)</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000"> ... 𝑟</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑚</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">) :</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000"> 𝑙𝑚</span>

<span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑤</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑐𝑚.𝑥,</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000"> 42)</span>
<span style="font-family:'DroidSans';font-size:5.98pt;color:#d9d9d9">Slide 51/86. «Java Memory Model Unlearning Experience», Aleksey Shipilёv, 2018, D:20180511133537+02’00’</span>

<span style="font-family:'F19';font-size:14.35pt;color:#000000">r1 =</span><span style="font-family:'F19';font-size:14.35pt;color:#0000ff"> lm</span><span style="font-family:'F19';font-size:14.35pt;color:#000000">.x</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">else</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">r1 = 1</span>

<span style="font-family:'DroidSans';font-size:7.97pt;color:#000000">hb</span>
<span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000">−−→</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑟</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">(</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑙𝑚.𝑥</span><span style="font-family:'CMR12';font-size:14.35pt;color:#000000">) : 0</span>

---

## Page 83

<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">Finals: Example 5.2</span>

<span style="font-family:'F19';font-size:14.35pt;color:#000000">class M {</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000"> 𝑣𝑜𝑙𝑎𝑡𝑖𝑙𝑒</span> <span style="font-family:'F19';font-size:14.35pt;color:#000000">int x = 42; }</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">M</span><span style="font-family:'F19';font-size:14.35pt;color:#ff0000"> m</span><span style="font-family:'F19';font-size:14.35pt;color:#000000">;</span>
<span style="font-family:'F19';font-size:14.35pt;color:#ff0000">m</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> = new M()</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">M</span><span style="font-family:'F19';font-size:14.35pt;color:#0000ff"> lm</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> =</span><span style="font-family:'F19';font-size:14.35pt;color:#ff0000"> m</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">if (</span><span style="font-family:'F19';font-size:14.35pt;color:#0000ff">lm</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> != null)</span>

<span style="font-family:'F19';font-size:14.35pt;color:#000000">r1 =</span><span style="font-family:'F19';font-size:14.35pt;color:#0000ff"> lm</span><span style="font-family:'F19';font-size:14.35pt;color:#000000">.x</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">else</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">r1 = 1</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">volatile</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000"> /</span><span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000">∈</span><span style="font-family:'F19';font-size:14.35pt;color:#000000">final</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">final</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000"> /</span><span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000">∈</span><span style="font-family:'F19';font-size:14.35pt;color:#000000">volatile</span>

<span style="font-family:'DroidSans';font-size:5.98pt;color:#d9d9d9">Slide 51/86. «Java Memory Model Unlearning Experience», Aleksey Shipilёv, 2018, D:20180511133537+02’00’</span>

---

## Page 84

<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">Finals: Safe Construction</span>

<span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑤𝑟𝑖𝑡𝑒𝑠</span><span style="font-family:'CMMI10';font-size:9.96pt;font-style:italic;color:#000000">𝑓𝑖𝑛𝑎𝑙</span>

<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Two absolutely necessary things:</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Field is</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> final</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Constructor does not publish</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> this</span>
<span style="font-family:'DroidSans';font-size:5.98pt;color:#d9d9d9">Slide 52/86. «Java Memory Model Unlearning Experience», Aleksey Shipilёv, 2018, D:20180511133537+02’00’</span>

<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Special rule for</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> final</span><span style="font-family:'DroidSans';font-size:14.35pt;color:#000000"> ﬁelds:</span>

<span style="font-family:'DroidSans';font-size:7.97pt;color:#000000">hb</span>
<span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000">−−→</span><span style="font-family:'CMMI12';font-size:14.35pt;font-style:italic;color:#000000">𝑟𝑒𝑎𝑑𝑠</span><span style="font-family:'CMMI10';font-size:9.96pt;font-style:italic;color:#000000">𝑓𝑖𝑛𝑎𝑙</span>

<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">The derivation for that rule is complicated.</span>

---

## Page 85

<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">Finals: Benign Races</span>
<span style="font-family:'F44';font-size:11.96pt;color:#000000">V v</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">;</span><span style="font-family:'F45';font-size:11.96pt;color:#408080"> // deliberately non-volatile</span>
<span style="font-family:'F44';font-size:11.96pt;color:#008000">public</span><span style="font-family:'F44';font-size:11.96pt;color:#000000"> V</span><span style="font-family:'F44';font-size:11.96pt;color:#0000ff"> racyRacy</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">() {</span>
<span style="font-family:'F44';font-size:11.96pt;color:#000000">V lv</span><span style="font-family:'F44';font-size:11.96pt;color:#666666"> =</span><span style="font-family:'F44';font-size:11.96pt;color:#000000"> v</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">;</span>
<span style="font-family:'F45';font-size:11.96pt;color:#408080">// RULE 1: Read it once (racily)</span>
<span style="font-family:'F44';font-size:11.96pt;color:#008000">if</span><span style="font-family:'F44';font-size:11.96pt;color:#666666"> (</span><span style="font-family:'F44';font-size:11.96pt;color:#000000">lv</span><span style="font-family:'F44';font-size:11.96pt;color:#666666"> ==</span><span style="font-family:'F44';font-size:11.96pt;color:#008000"> null</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">) {</span>
<span style="font-family:'F45';font-size:11.96pt;color:#408080">// RULE 2: Check it is fine</span>
<span style="font-family:'F44';font-size:11.96pt;color:#000000">v</span><span style="font-family:'F44';font-size:11.96pt;color:#666666"> =</span><span style="font-family:'F44';font-size:11.96pt;color:#000000"> compute</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">();</span>
<span style="font-family:'F45';font-size:11.96pt;color:#408080">// RULE 3: Recover by safely constructing</span>
<span style="font-family:'F44';font-size:11.96pt;color:#666666">}</span>
<span style="font-family:'F44';font-size:11.96pt;color:#008000">return</span><span style="font-family:'F44';font-size:11.96pt;color:#000000"> lv</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">;</span>
<span style="font-family:'F44';font-size:11.96pt;color:#666666">}</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Forgo one of the rules, and you get the</span><span style="font-family:'DroidSans-Bold';font-size:14.35pt;font-weight:bold;color:#000000"> non-benign</span><span style="font-family:'DroidSans';font-size:14.35pt;color:#000000"> race.</span>
<span style="font-family:'DroidSans';font-size:5.98pt;color:#d9d9d9">Slide 53/86. «Java Memory Model Unlearning Experience», Aleksey Shipilёv, 2018, D:20180511133537+02’00’</span>

---

## Page 86

<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">Finals: Benign Races, Real Example</span>
<span style="font-family:'F44';font-size:11.96pt;color:#008000">public class</span><span style="font-family:'F44';font-size:11.96pt;color:#0000ff"> AbstractMap</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">&lt;</span><span style="font-family:'F44';font-size:11.96pt;color:#000000">K</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">,</span><span style="font-family:'F44';font-size:11.96pt;color:#000000"> V</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">&gt; {</span>
<span style="font-family:'F44';font-size:11.96pt;color:#008000">transient</span><span style="font-family:'F44';font-size:11.96pt;color:#000000"> Set</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">&lt;</span><span style="font-family:'F44';font-size:11.96pt;color:#000000">K</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">&gt;</span><span style="font-family:'F44';font-size:11.96pt;color:#000000"> keySet</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">;</span><span style="font-family:'F45';font-size:11.96pt;color:#408080"> // non-volatile</span>
<span style="font-family:'F44';font-size:11.96pt;color:#008000">public</span><span style="font-family:'F44';font-size:11.96pt;color:#000000"> Set</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">&lt;</span><span style="font-family:'F44';font-size:11.96pt;color:#000000">K</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">&gt;</span><span style="font-family:'F44';font-size:11.96pt;color:#0000ff"> keySet</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">() {</span>
<span style="font-family:'F44';font-size:11.96pt;color:#000000">Set</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">&lt;</span><span style="font-family:'F44';font-size:11.96pt;color:#000000">K</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">&gt;</span><span style="font-family:'F44';font-size:11.96pt;color:#000000"> ks</span><span style="font-family:'F44';font-size:11.96pt;color:#666666"> =</span><span style="font-family:'F44';font-size:11.96pt;color:#000000"> keySet</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">;</span>
<span style="font-family:'F45';font-size:11.96pt;color:#408080">// RULE 1: Read it once (racily)</span>
<span style="font-family:'F44';font-size:11.96pt;color:#008000">if</span><span style="font-family:'F44';font-size:11.96pt;color:#666666"> (</span><span style="font-family:'F44';font-size:11.96pt;color:#000000">ks</span><span style="font-family:'F44';font-size:11.96pt;color:#666666"> ==</span><span style="font-family:'F44';font-size:11.96pt;color:#008000"> null</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">) {</span>
<span style="font-family:'F45';font-size:11.96pt;color:#408080">// RULE 2: Check it’s fine</span>
<span style="font-family:'F44';font-size:11.96pt;color:#000000">ks</span><span style="font-family:'F44';font-size:11.96pt;color:#666666"> =</span><span style="font-family:'F44';font-size:11.96pt;color:#008000"> new</span><span style="font-family:'F44';font-size:11.96pt;color:#000000"> KeySet</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">();</span><span style="font-family:'F45';font-size:11.96pt;color:#408080"> // RULE 3: Recover by safely constructing</span>
<span style="font-family:'F44';font-size:11.96pt;color:#000000">keySet</span><span style="font-family:'F44';font-size:11.96pt;color:#666666"> =</span><span style="font-family:'F44';font-size:11.96pt;color:#000000"> ks</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">;</span>
<span style="font-family:'F44';font-size:11.96pt;color:#666666">}</span>
<span style="font-family:'F44';font-size:11.96pt;color:#008000">return</span><span style="font-family:'F44';font-size:11.96pt;color:#000000"> ks</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">;</span>
<span style="font-family:'F44';font-size:11.96pt;color:#666666">}</span>
<span style="font-family:'F44';font-size:11.96pt;color:#666666">}</span>
<span style="font-family:'DroidSans';font-size:5.98pt;color:#d9d9d9">Slide 54/86. «Java Memory Model Unlearning Experience», Aleksey Shipilёv, 2018, D:20180511133537+02’00’</span>

---

## Page 87

<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">Finals: Takeaway #7</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">1. Safe construction is another major (and simple) rule</span>

<span style="font-family:'DroidSans';font-size:11.96pt;color:#000000">Use it to protect against inadvertent races!</span>
<span style="font-family:'DroidSans';font-size:11.96pt;color:#000000">When it doubt, make all ﬁelds</span><span style="font-family:'F44';font-size:11.96pt;color:#000000"> final</span>

<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">2. Benign races are seldom useful</span>

<span style="font-family:'DroidSans';font-size:11.96pt;color:#000000">Allow avoiding synchronized ops on critical paths</span>
<span style="font-family:'DroidSans';font-size:11.96pt;color:#000000">Work only if three rules are followed: single (racy) read,</span>
<span style="font-family:'DroidSans';font-size:11.96pt;color:#000000">reliability check, recovery path that safely constructs</span>

<span style="font-family:'DroidSans';font-size:5.98pt;color:#d9d9d9">Slide 55/86. «Java Memory Model Unlearning Experience», Aleksey Shipilёv, 2018, D:20180511133537+02’00’</span>

---

## Page 88

Locks

<!-- 图源: ./geecon-May2018-jmm_assets/images/p088_scan.jpg -->


<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">Locks</span>

---

## Page 89

<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">Locks: JMM and Ordering Modes</span>

<span style="font-family:'F19';font-size:14.35pt;color:#000000">Java 8</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">Java 9</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">–</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">–</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">N</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">N</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">N</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">N</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">N</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">plain</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">VH Plain</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">Y</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">N</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">N</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">N</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">N</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">–</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">VH Opaque</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">Y</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">Y</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">N</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">N</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">N</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">–</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">VH Acq/Rel</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">Y</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">Y</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">Y</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">N</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">N</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">volatile</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">VH SeqCst</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">Y</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">Y</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">Y</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">Y</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">N</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">locks</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">–</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">Y</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">Y</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">Y</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">Y</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">Y</span>

<span style="font-family:'DroidSans';font-size:5.98pt;color:#d9d9d9">Slide 57/86. «Java Memory Model Unlearning Experience», Aleksey Shipilёv, 2018, D:20180511133537+02’00’</span>

<span style="font-family:'F19';font-size:14.35pt;color:#000000">Mutual Excl</span>

<span style="font-family:'F19';font-size:14.35pt;color:#000000">Coherence</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">Causality</span>

<span style="font-family:'F19';font-size:14.35pt;color:#000000">Consensus</span>

<span style="font-family:'F19';font-size:14.35pt;color:#000000">Definite</span>

---

## Page 90

Summing Up

<!-- 图源: ./geecon-May2018-jmm_assets/images/p090_scan.jpg -->


<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">Summing Up</span>

---

## Page 91

<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">Summing Up: Rule #1: Safe Publication</span>

<span style="font-family:'DroidSans-Bold';font-size:14.35pt;font-weight:bold;color:#000000">Golden Rule:</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Thread 1: store everything, then</span><span style="font-family:'DroidSans-Bold';font-size:14.35pt;font-weight:bold;color:#000000"> release</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Thread 2:</span><span style="font-family:'DroidSans-Bold';font-size:14.35pt;font-weight:bold;color:#000000"> acquire</span><span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">, then read anything</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Automatically happens when publishing via</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">well-designed concurrency primitives</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Has to happen on</span><span style="font-family:'DroidSans-Bold';font-size:14.35pt;font-weight:bold;color:#000000"> all possible</span><span style="font-family:'DroidSans';font-size:14.35pt;color:#000000"> execution paths</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Has to happen in correct order</span>
<span style="font-family:'DroidSans';font-size:5.98pt;color:#d9d9d9">Slide 59/86. «Java Memory Model Unlearning Experience», Aleksey Shipilёv, 2018, D:20180511133537+02’00’</span>

---

## Page 92

<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">Summing Up: Rule #2: Safe Construction</span>

<span style="font-family:'DroidSans-Bold';font-size:14.35pt;font-weight:bold;color:#000000">Golden Rule:</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">When in doubt, make all ﬁelds</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> final</span><span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">.</span>

<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Makes the whole thing more resilient to races</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Think «defense in depth»: survive in case some path fails</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">to publish the instance safely</span>

<span style="font-family:'DroidSans';font-size:5.98pt;color:#d9d9d9">Slide 60/86. «Java Memory Model Unlearning Experience», Aleksey Shipilёv, 2018, D:20180511133537+02’00’</span>

---

## Page 93

<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">Summing Up: Rule #3: Benign Races</span>

<span style="font-family:'DroidSans-Bold';font-size:14.35pt;font-weight:bold;color:#000000">Golden Rule:</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Object is safely constructed, and there is single read.</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Exotic optimization technique, rarely needed</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">The (only) easy way to avoid synchronization</span>

<span style="font-family:'DroidSans';font-size:5.98pt;color:#d9d9d9">Slide 61/86. «Java Memory Model Unlearning Experience», Aleksey Shipilёv, 2018, D:20180511133537+02’00’</span>

---

## Page 94

<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">Summing Up: Rule #4: Exotic Modes</span>

<span style="font-family:'DroidSans-Bold';font-size:14.35pt;font-weight:bold;color:#000000">Golden Rule:</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Don’t.</span>

<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Just don’t!</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">There are cases where performance is so important, you</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">want to have weaker than</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> volatile</span><span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">, but stronger than</span>
<span style="font-family:'F19';font-size:14.35pt;color:#000000">plain</span><span style="font-family:'DroidSans';font-size:14.35pt;color:#000000"> –</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> VarHandles</span><span style="font-family:'DroidSans';font-size:14.35pt;color:#000000"> to rescue!</span>
<span style="font-family:'DroidSans';font-size:5.98pt;color:#d9d9d9">Slide 62/86. «Java Memory Model Unlearning Experience», Aleksey Shipilёv, 2018, D:20180511133537+02’00’</span>

---

## Page 95

Practice

<!-- 图源: ./geecon-May2018-jmm_assets/images/p095_scan.jpg -->


<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">Practice</span>

---

## Page 96

<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">Practice: Double-Checked Locking</span>
<span style="font-family:'F44';font-size:11.96pt;color:#008000">volatile</span><span style="font-family:'F44';font-size:11.96pt;color:#000000"> T val</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">;</span>
<span style="font-family:'F44';font-size:11.96pt;color:#008000">public</span><span style="font-family:'F44';font-size:11.96pt;color:#000000"> T</span><span style="font-family:'F44';font-size:11.96pt;color:#0000ff"> get</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">() {</span>
<span style="font-family:'F44';font-size:11.96pt;color:#008000">if</span><span style="font-family:'F44';font-size:11.96pt;color:#666666"> (</span><span style="font-family:'F44';font-size:11.96pt;color:#000000"> 1</span>
<span style="font-family:'F44';font-size:11.96pt;color:#000000">val</span><span style="font-family:'F44';font-size:11.96pt;color:#666666"> ==</span><span style="font-family:'F44';font-size:11.96pt;color:#008000"> null</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">) {</span>
<span style="font-family:'F44';font-size:11.96pt;color:#008000">synchronized</span><span style="font-family:'F44';font-size:11.96pt;color:#666666"> (</span><span style="font-family:'F44';font-size:11.96pt;color:#008000">this</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">) {</span>

<span style="font-family:'F44';font-size:11.96pt;color:#008000">if</span><span style="font-family:'F44';font-size:11.96pt;color:#666666"> (</span><span style="font-family:'F44';font-size:11.96pt;color:#000000"> 2</span>
<span style="font-family:'F44';font-size:11.96pt;color:#000000">val</span><span style="font-family:'F44';font-size:11.96pt;color:#666666"> ==</span><span style="font-family:'F44';font-size:11.96pt;color:#008000"> null</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">) {</span>
<span style="font-family:'F44';font-size:11.96pt;color:#000000">3</span>
<span style="font-family:'F44';font-size:11.96pt;color:#000000">val</span><span style="font-family:'F44';font-size:11.96pt;color:#666666"> =</span><span style="font-family:'F44';font-size:11.96pt;color:#008000"> new</span><span style="font-family:'F44';font-size:11.96pt;color:#000000"> T</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">();</span>
<span style="font-family:'F44';font-size:11.96pt;color:#666666">}</span>
<span style="font-family:'F44';font-size:11.96pt;color:#666666">}</span>
<span style="font-family:'F44';font-size:11.96pt;color:#666666">}</span>
<span style="font-family:'F44';font-size:11.96pt;color:#008000">return</span>
<span style="font-family:'F44';font-size:11.96pt;color:#000000">4</span>
<span style="font-family:'F44';font-size:11.96pt;color:#000000">val</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">;</span>
<span style="font-family:'F44';font-size:11.96pt;color:#666666">}</span>
<span style="font-family:'DroidSans';font-size:5.98pt;color:#d9d9d9">Slide 64/86. «Java Memory Model Unlearning Experience», Aleksey Shipilёv, 2018, D:20180511133537+02’00’</span>

---

## Page 97

<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">Practice: Double-Checked Locking</span>
<span style="font-family:'F44';font-size:11.96pt;color:#008000">volatile</span><span style="font-family:'F44';font-size:11.96pt;color:#000000"> T val</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">;</span>
<span style="font-family:'F44';font-size:11.96pt;color:#008000">public</span><span style="font-family:'F44';font-size:11.96pt;color:#000000"> T</span><span style="font-family:'F44';font-size:11.96pt;color:#0000ff"> get</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">() {</span>
<span style="font-family:'F44';font-size:11.96pt;color:#008000">if</span><span style="font-family:'F44';font-size:11.96pt;color:#666666"> (</span><span style="font-family:'F44';font-size:11.96pt;color:#000000"> 1</span>
<span style="font-family:'F44';font-size:11.96pt;color:#000000">val</span><span style="font-family:'F44';font-size:11.96pt;color:#666666"> ==</span><span style="font-family:'F44';font-size:11.96pt;color:#008000"> null</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">) {</span>
<span style="font-family:'F44';font-size:11.96pt;color:#008000">synchronized</span><span style="font-family:'F44';font-size:11.96pt;color:#666666"> (</span><span style="font-family:'F44';font-size:11.96pt;color:#008000">this</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">) {</span>

<span style="font-family:'F44';font-size:11.96pt;color:#008000">if</span><span style="font-family:'F44';font-size:11.96pt;color:#666666"> (</span><span style="font-family:'F44';font-size:11.96pt;color:#000000"> 2</span>
<span style="font-family:'F44';font-size:11.96pt;color:#000000">val</span><span style="font-family:'F44';font-size:11.96pt;color:#666666"> ==</span><span style="font-family:'F44';font-size:11.96pt;color:#008000"> null</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">) {</span>
<span style="font-family:'F44';font-size:11.96pt;color:#000000">3</span>
<span style="font-family:'F44';font-size:11.96pt;color:#000000">val</span><span style="font-family:'F44';font-size:11.96pt;color:#666666"> =</span><span style="font-family:'F44';font-size:11.96pt;color:#008000"> new</span><span style="font-family:'F44';font-size:11.96pt;color:#000000"> T</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">();</span>
<span style="font-family:'F44';font-size:11.96pt;color:#666666">}</span>
<span style="font-family:'F44';font-size:11.96pt;color:#666666">}</span>
<span style="font-family:'F44';font-size:11.96pt;color:#666666">}</span>
<span style="font-family:'F44';font-size:11.96pt;color:#008000">return</span>
<span style="font-family:'F44';font-size:11.96pt;color:#000000">4</span>
<span style="font-family:'F44';font-size:11.96pt;color:#000000">val</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">;</span>
<span style="font-family:'F44';font-size:11.96pt;color:#666666">}</span>
<span style="font-family:'DroidSans';font-size:5.98pt;color:#d9d9d9">Slide 64/86. «Java Memory Model Unlearning Experience», Aleksey Shipilёv, 2018, D:20180511133537+02’00’</span>

<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Holy Macaroni, it does not</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">work without</span><span style="font-family:'F19';font-size:14.35pt;color:#000000"> volatile</span><span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">!</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">But why do you need it?</span>

---

## Page 98

<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">Practice: Double-Checked Locking</span>
<span style="font-family:'F44';font-size:11.96pt;color:#008000">volatile</span><span style="font-family:'F44';font-size:11.96pt;color:#000000"> T val</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">;</span>
<span style="font-family:'F44';font-size:11.96pt;color:#008000">public</span><span style="font-family:'F44';font-size:11.96pt;color:#000000"> T</span><span style="font-family:'F44';font-size:11.96pt;color:#0000ff"> get</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">() {</span>
<span style="font-family:'F44';font-size:11.96pt;color:#008000">if</span><span style="font-family:'F44';font-size:11.96pt;color:#666666"> (</span><span style="font-family:'F44';font-size:11.96pt;color:#000000"> 1</span>
<span style="font-family:'F44';font-size:11.96pt;color:#000000">val</span><span style="font-family:'F44';font-size:11.96pt;color:#666666"> ==</span><span style="font-family:'F44';font-size:11.96pt;color:#008000"> null</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">) {</span>
<span style="font-family:'F44';font-size:11.96pt;color:#008000">synchronized</span><span style="font-family:'F44';font-size:11.96pt;color:#666666"> (</span><span style="font-family:'F44';font-size:11.96pt;color:#008000">this</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">) {</span>

<span style="font-family:'F44';font-size:11.96pt;color:#008000">if</span><span style="font-family:'F44';font-size:11.96pt;color:#666666"> (</span><span style="font-family:'F44';font-size:11.96pt;color:#000000"> 2</span>
<span style="font-family:'F44';font-size:11.96pt;color:#000000">val</span><span style="font-family:'F44';font-size:11.96pt;color:#666666"> ==</span><span style="font-family:'F44';font-size:11.96pt;color:#008000"> null</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">) {</span>
<span style="font-family:'F44';font-size:11.96pt;color:#000000">3</span>
<span style="font-family:'F44';font-size:11.96pt;color:#000000">val</span><span style="font-family:'F44';font-size:11.96pt;color:#666666"> =</span><span style="font-family:'F44';font-size:11.96pt;color:#008000"> new</span><span style="font-family:'F44';font-size:11.96pt;color:#000000"> T</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">();</span>
<span style="font-family:'F44';font-size:11.96pt;color:#666666">}</span>
<span style="font-family:'F44';font-size:11.96pt;color:#666666">}</span>
<span style="font-family:'F44';font-size:11.96pt;color:#666666">}</span>
<span style="font-family:'F44';font-size:11.96pt;color:#008000">return</span>
<span style="font-family:'F44';font-size:11.96pt;color:#000000">4</span>
<span style="font-family:'F44';font-size:11.96pt;color:#000000">val</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">;</span>
<span style="font-family:'F44';font-size:11.96pt;color:#666666">}</span>
<span style="font-family:'DroidSans';font-size:5.98pt;color:#d9d9d9">Slide 64/86. «Java Memory Model Unlearning Experience», Aleksey Shipilёv, 2018, D:20180511133537+02’00’</span>

<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">What ordering modes are</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">necessary at 1, 2, 3, 4?</span>

---

## Page 99

<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">Practice: Double-Checked Locking</span>
<span style="font-family:'F44';font-size:11.96pt;color:#008000">volatile</span><span style="font-family:'F44';font-size:11.96pt;color:#000000"> T val</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">;</span>
<span style="font-family:'F44';font-size:11.96pt;color:#008000">public</span><span style="font-family:'F44';font-size:11.96pt;color:#000000"> T</span><span style="font-family:'F44';font-size:11.96pt;color:#0000ff"> get</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">() {</span>
<span style="font-family:'F44';font-size:11.96pt;color:#008000">if</span><span style="font-family:'F44';font-size:11.96pt;color:#666666"> (</span><span style="font-family:'F44';font-size:11.96pt;color:#000000"> 1</span>
<span style="font-family:'F44';font-size:11.96pt;color:#000000">val</span><span style="font-family:'F44';font-size:11.96pt;color:#666666"> ==</span><span style="font-family:'F44';font-size:11.96pt;color:#008000"> null</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">) {</span>
<span style="font-family:'F44';font-size:11.96pt;color:#008000">synchronized</span><span style="font-family:'F44';font-size:11.96pt;color:#666666"> (</span><span style="font-family:'F44';font-size:11.96pt;color:#008000">this</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">) {</span>

<span style="font-family:'F44';font-size:11.96pt;color:#008000">if</span><span style="font-family:'F44';font-size:11.96pt;color:#666666"> (</span><span style="font-family:'F44';font-size:11.96pt;color:#000000"> 2</span>
<span style="font-family:'F44';font-size:11.96pt;color:#000000">val</span><span style="font-family:'F44';font-size:11.96pt;color:#666666"> ==</span><span style="font-family:'F44';font-size:11.96pt;color:#008000"> null</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">) {</span>
<span style="font-family:'F44';font-size:11.96pt;color:#000000">3</span>
<span style="font-family:'F44';font-size:11.96pt;color:#000000">val</span><span style="font-family:'F44';font-size:11.96pt;color:#666666"> =</span><span style="font-family:'F44';font-size:11.96pt;color:#008000"> new</span><span style="font-family:'F44';font-size:11.96pt;color:#000000"> T</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">();</span>
<span style="font-family:'F44';font-size:11.96pt;color:#666666">}</span>
<span style="font-family:'F44';font-size:11.96pt;color:#666666">}</span>
<span style="font-family:'F44';font-size:11.96pt;color:#666666">}</span>
<span style="font-family:'F44';font-size:11.96pt;color:#008000">return</span>
<span style="font-family:'F44';font-size:11.96pt;color:#000000">4</span>
<span style="font-family:'F44';font-size:11.96pt;color:#000000">val</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">;</span>
<span style="font-family:'F44';font-size:11.96pt;color:#666666">}</span>
<span style="font-family:'DroidSans';font-size:5.98pt;color:#d9d9d9">Slide 64/86. «Java Memory Model Unlearning Experience», Aleksey Shipilёv, 2018, D:20180511133537+02’00’</span>

<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">What ordering modes are</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">necessary at 1, 2, 3, 4?</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Release/acquire: 3</span><span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000"> →</span><span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">1</span>

---

## Page 100

<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">Practice: Double-Checked Locking</span>
<span style="font-family:'F44';font-size:11.96pt;color:#008000">volatile</span><span style="font-family:'F44';font-size:11.96pt;color:#000000"> T val</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">;</span>
<span style="font-family:'F44';font-size:11.96pt;color:#008000">public</span><span style="font-family:'F44';font-size:11.96pt;color:#000000"> T</span><span style="font-family:'F44';font-size:11.96pt;color:#0000ff"> get</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">() {</span>
<span style="font-family:'F44';font-size:11.96pt;color:#008000">if</span><span style="font-family:'F44';font-size:11.96pt;color:#666666"> (</span><span style="font-family:'F44';font-size:11.96pt;color:#000000"> 1</span>
<span style="font-family:'F44';font-size:11.96pt;color:#000000">val</span><span style="font-family:'F44';font-size:11.96pt;color:#666666"> ==</span><span style="font-family:'F44';font-size:11.96pt;color:#008000"> null</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">) {</span>
<span style="font-family:'F44';font-size:11.96pt;color:#008000">synchronized</span><span style="font-family:'F44';font-size:11.96pt;color:#666666"> (</span><span style="font-family:'F44';font-size:11.96pt;color:#008000">this</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">) {</span>

<span style="font-family:'F44';font-size:11.96pt;color:#008000">if</span><span style="font-family:'F44';font-size:11.96pt;color:#666666"> (</span><span style="font-family:'F44';font-size:11.96pt;color:#000000"> 2</span>
<span style="font-family:'F44';font-size:11.96pt;color:#000000">val</span><span style="font-family:'F44';font-size:11.96pt;color:#666666"> ==</span><span style="font-family:'F44';font-size:11.96pt;color:#008000"> null</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">) {</span>
<span style="font-family:'F44';font-size:11.96pt;color:#000000">3</span>
<span style="font-family:'F44';font-size:11.96pt;color:#000000">val</span><span style="font-family:'F44';font-size:11.96pt;color:#666666"> =</span><span style="font-family:'F44';font-size:11.96pt;color:#008000"> new</span><span style="font-family:'F44';font-size:11.96pt;color:#000000"> T</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">();</span>
<span style="font-family:'F44';font-size:11.96pt;color:#666666">}</span>
<span style="font-family:'F44';font-size:11.96pt;color:#666666">}</span>
<span style="font-family:'F44';font-size:11.96pt;color:#666666">}</span>
<span style="font-family:'F44';font-size:11.96pt;color:#008000">return</span>
<span style="font-family:'F44';font-size:11.96pt;color:#000000">4</span>
<span style="font-family:'F44';font-size:11.96pt;color:#000000">val</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">;</span>
<span style="font-family:'F44';font-size:11.96pt;color:#666666">}</span>
<span style="font-family:'DroidSans';font-size:5.98pt;color:#d9d9d9">Slide 64/86. «Java Memory Model Unlearning Experience», Aleksey Shipilёv, 2018, D:20180511133537+02’00’</span>

<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">What ordering modes are</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">necessary at 1, 2, 3, 4?</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Release/acquire: 3</span><span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000"> →</span><span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">1</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Coherence: 1</span><span style="font-family:'CMSY10';font-size:14.35pt;font-style:italic;color:#000000"> →</span><span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">4</span>

---

## Page 101

<span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">Practice: DCL with VarHandles</span>

<span style="font-family:'F44';font-size:11.96pt;color:#008000">static final</span><span style="font-family:'F44';font-size:11.96pt;color:#000000"> VH</span><span style="font-family:'F44';font-size:11.96pt;color:#666666"> = ...;</span>
<span style="font-family:'F44';font-size:11.96pt;color:#000000">V val</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">;</span><span style="font-family:'F45';font-size:11.96pt;color:#408080"> // not volatile, specify at use-site</span>
<span style="font-family:'F44';font-size:11.96pt;color:#008000">public</span><span style="font-family:'F44';font-size:11.96pt;color:#000000"> V</span><span style="font-family:'F44';font-size:11.96pt;color:#0000ff"> get</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">() {</span>
<span style="font-family:'F44';font-size:11.96pt;color:#008000">if</span><span style="font-family:'F44';font-size:11.96pt;color:#666666"> (</span><span style="font-family:'F44';font-size:11.96pt;color:#000000">VH</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">.</span><span style="font-family:'F44';font-size:11.96pt;color:#7d8f29">getAcquire</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">(</span><span style="font-family:'F44';font-size:11.96pt;color:#008000">this</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">) ==</span><span style="font-family:'F44';font-size:11.96pt;color:#008000"> null</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">) {</span>
<span style="font-family:'F44';font-size:11.96pt;color:#008000">synchronized</span><span style="font-family:'F44';font-size:11.96pt;color:#666666"> (</span><span style="font-family:'F44';font-size:11.96pt;color:#008000">this</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">) {</span>
<span style="font-family:'F44';font-size:11.96pt;color:#008000">if</span><span style="font-family:'F44';font-size:11.96pt;color:#666666"> (</span><span style="font-family:'F44';font-size:11.96pt;color:#000000">VH</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">.</span><span style="font-family:'F44';font-size:11.96pt;color:#7d8f29">get</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">(</span><span style="font-family:'F44';font-size:11.96pt;color:#008000">this</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">) ==</span><span style="font-family:'F44';font-size:11.96pt;color:#008000"> null</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">) {</span>
<span style="font-family:'F44';font-size:11.96pt;color:#000000">VH</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">.</span><span style="font-family:'F44';font-size:11.96pt;color:#7d8f29">setRelease</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">(</span><span style="font-family:'F44';font-size:11.96pt;color:#008000">this</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">,</span><span style="font-family:'F44';font-size:11.96pt;color:#008000"> new</span><span style="font-family:'F44';font-size:11.96pt;color:#000000"> T</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">());</span>
<span style="font-family:'F44';font-size:11.96pt;color:#666666">}</span>
<span style="font-family:'F44';font-size:11.96pt;color:#666666">}</span>
<span style="font-family:'F44';font-size:11.96pt;color:#666666">}</span>
<span style="font-family:'F44';font-size:11.96pt;color:#008000">return</span><span style="font-family:'F44';font-size:11.96pt;color:#000000"> VH</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">.</span><span style="font-family:'F44';font-size:11.96pt;color:#7d8f29">get</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">(</span><span style="font-family:'F44';font-size:11.96pt;color:#008000">this</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">);</span>
<span style="font-family:'F44';font-size:11.96pt;color:#666666">}</span>

<span style="font-family:'DroidSans';font-size:5.98pt;color:#d9d9d9">Slide 65/86. «Java Memory Model Unlearning Experience», Aleksey Shipilёv, 2018, D:20180511133537+02’00’</span>

---

## Page 102

<span style="font-family:'F126';font-size:17.22pt;color:#000000">Lazy&lt;V&gt;</span><span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">: The Purest Form</span>
<span style="font-family:'F44';font-size:11.96pt;color:#008000">public class</span><span style="font-family:'F44';font-size:11.96pt;color:#0000ff"> Lazy</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">&lt;</span><span style="font-family:'F44';font-size:11.96pt;color:#000000">V</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">&gt; {</span>
<span style="font-family:'F44';font-size:11.96pt;color:#008000">final</span><span style="font-family:'F44';font-size:11.96pt;color:#000000"> Supplier</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">&lt;</span><span style="font-family:'F44';font-size:11.96pt;color:#000000">V</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">&gt;</span><span style="font-family:'F44';font-size:11.96pt;color:#000000"> s</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">;</span>
<span style="font-family:'F44';font-size:11.96pt;color:#000000">V v</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">;</span>
<span style="font-family:'F44';font-size:11.96pt;color:#008000">public</span><span style="font-family:'F44';font-size:11.96pt;color:#0000ff"> Lazy</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">(</span><span style="font-family:'F44';font-size:11.96pt;color:#000000">Supplier</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">&lt;</span><span style="font-family:'F44';font-size:11.96pt;color:#000000">V</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">&gt;</span><span style="font-family:'F44';font-size:11.96pt;color:#000000"> s</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">) {</span>
<span style="font-family:'F44';font-size:11.96pt;color:#008000">this</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">.</span><span style="font-family:'F44';font-size:11.96pt;color:#7d8f29">s</span><span style="font-family:'F44';font-size:11.96pt;color:#666666"> =</span><span style="font-family:'F44';font-size:11.96pt;color:#000000"> s</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">;</span>
<span style="font-family:'F44';font-size:11.96pt;color:#666666">}</span>
<span style="font-family:'F44';font-size:11.96pt;color:#008000">public synchronized</span><span style="font-family:'F44';font-size:11.96pt;color:#000000"> V</span><span style="font-family:'F44';font-size:11.96pt;color:#0000ff"> get</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">() {</span>
<span style="font-family:'F44';font-size:11.96pt;color:#008000">if</span><span style="font-family:'F44';font-size:11.96pt;color:#666666"> (</span><span style="font-family:'F44';font-size:11.96pt;color:#000000">v</span><span style="font-family:'F44';font-size:11.96pt;color:#666666"> ==</span><span style="font-family:'F44';font-size:11.96pt;color:#008000"> null</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">) {</span>
<span style="font-family:'F44';font-size:11.96pt;color:#000000">v</span><span style="font-family:'F44';font-size:11.96pt;color:#666666"> =</span><span style="font-family:'F44';font-size:11.96pt;color:#000000"> s</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">.</span><span style="font-family:'F44';font-size:11.96pt;color:#7d8f29">get</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">();</span>
<span style="font-family:'F44';font-size:11.96pt;color:#666666">}</span>
<span style="font-family:'F44';font-size:11.96pt;color:#008000">return</span><span style="font-family:'F44';font-size:11.96pt;color:#000000"> v</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">;</span>
<span style="font-family:'F44';font-size:11.96pt;color:#666666">}</span>
<span style="font-family:'F44';font-size:11.96pt;color:#666666">}</span>
<span style="font-family:'DroidSans';font-size:5.98pt;color:#d9d9d9">Slide 66/86. «Java Memory Model Unlearning Experience», Aleksey Shipilёv, 2018, D:20180511133537+02’00’</span>

<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">Lazy instantiator:</span>
<span style="font-family:'DroidSans';font-size:14.35pt;color:#000000">obviously correct, right?</span>

---

## Page 103

<span style="font-family:'F126';font-size:17.22pt;color:#000000">Lazy&lt;V&gt;</span><span style="font-family:'DroidSans-Bold';font-size:17.22pt;font-weight:bold;color:#000000">: The Purest Form</span>
<span style="font-family:'F44';font-size:11.96pt;color:#008000">public class</span><span style="font-family:'F44';font-size:11.96pt;color:#0000ff"> Lazy</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">&lt;</span><span style="font-family:'F44';font-size:11.96pt;color:#000000">V</span><span style="font-family:'F44';font-size:11.96pt;color:#666666">