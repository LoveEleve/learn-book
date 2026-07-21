## Page 1

TURING
[9特] 态伊 - 阿拉马尔 ( Jay Alamr
[期马尔
图解  李博杰孟佳顺
DeepSeek技术
TheIllustratedDeepSeek

近120幅全彩插图
理解推理大模型的原理

中国工信出版集团重人民邮电出教社

<!-- 图源: ./图解DeepSeek技术_assets/images/p001_scan.jpg -->

---

## Page 2

<span style="font-family:'Unnamed-T3';font-size:21.25pt;color:#000000">版权信息</span>
<span style="font-family:'Unnamed-T3';font-size:8.75pt;color:#000000">COPYRIGHT</span>

<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">书名：图解DeepSeek技术</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">作者：【沙特】杰伊·阿拉马尔【荷】马尔滕·格鲁滕多斯特</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">译者：李博杰；孟佳颖</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">出版社：人民邮电出版社</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">出版时间：2025年6月</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">ISBN：9787115674616</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">字数：31千字</span>

---

## Page 3

<span style="font-family:'Unnamed-T3';font-size:21.25pt;color:#000000">译者序</span>

<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">DeepSeek-R1的发布可以说是AI（artificial</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">intelligence，人工智</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">能）领域的第二个“ChatGPT时刻”。GPT-3.5的RLHF（基于人类</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">反馈的强化学习）让AI学会了回答问题，而OpenAI</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">o1和DeepSeek-</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">R1的强化学习后训练让AI学会了思考。DeepSeek-R1首次系统性地</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">把这一方法背后的原理公之于众，且在创意写作等领域达到了世界领</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">先水平，体现了中国在AI领域的持续创新和实践。</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">DeepSeek-R1不仅为世界贡献了强大的开源推理语言模型，让推理大</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">模型的使用成本下降了一个数量级，更重要的是，它的出现揭示了两</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">个关键的事实。</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">第一，强化学习可能成为一些公司的“秘密武器”。在强大的基座模</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">型的基础上，强化学习后训练只需要相对较少的算力，就可以把复杂</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">的提示词内化成模型的内生能力，解决在提示词复杂的情况下指令遵</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">循可靠性差的问题，大幅提升模型解决特定领域问题的可靠性。一些</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">实力强的公司还能利用强化学习，基于用户的反馈数据，实现模型对</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">主流用户偏好的对齐，构建数据飞轮。对一些需要本地化部署或需要</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">控制推理成本的场景，如果只关注特定领域能力而非泛化能力，那么</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">在开源模型的基础上进行监督微调和强化学习，有望达到与最强大的</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">闭源模型相当的领域能力。</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">第二，DeepSeek-R1-Zero更是指出了一条让AI像人类一样，通过与</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">环境交互不断自我进化的路线。如今的大模型几乎已穷尽高质量的预</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">训练语料，而强化学习后训练理论上可以生成无限的语料。</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">DeepSeek-R1-Zero只需构建一个让AI自主探索并验证结果对错的环</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">境，无须人类显式指导，强化学习算法就可以通过试错法找到问题的</span>

---

## Page 4

<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">可行解，并将其内化成模型参数，从而在后续遇到类似问题时，更可</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">靠和快速地加以解决。</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">本书的原作是由《图解大模型：生成式AI原理与实战》的两位作者</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">Jay</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">Alammar和Maarten</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">Grootendorst专门为中国读者撰写的，经</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">过笔者翻译后才得以面世，因此，这不是一本普通的译著。关于</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">DeepSeek-R1的资料不计其数，但尚未见到像本书一样图示精美、通</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">俗易懂又不失技术深度的解读。本书主要涵盖以下知识点：</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">推理大模型的基本原理</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">MoE架构设计（在DeepSeek-V2中提出）</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">DeepSeek-R1-Zero的训练过程</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">DeepSeek-V3的效率优化策略（MLA</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000"> </span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">V2中首次提出的核心创新之一，DeepSeek-V3在延续DeepSeek-V2架构的基础上，进一步</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">优化了MLA，并将其应用于更广泛的场景中。</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">、混合精度训练、多词元预测）</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">DeepSeek-R1的训练过程</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">基于GRPO的强化学习（在DeepSeek-R1中提出）</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">DeepSeek开源周</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">限于篇幅，本书没有展开强化学习后训练的细节和实战。如果读者需</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">要对自己的模型进化强化学习后训练，建议参考verl、OpenRLHF等</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">开源框架。此外，本书也没有涵盖系统性能优化相关的内容。</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">为了帮助大家更好地理解本书，围绕推理大模型和DeepSeek，我们</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">系统梳理了大家最关注的一系列问题，其中大多数问题可以在书中直</span>

<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">MLA是DeepSeek团队在DeepSeek-</span>

![](./图解DeepSeek技术_assets/images/p004_img01.png)

---

## Page 5

<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">接找到答案，部分问题可以从DeepSeek原始论文和本书的参考资料</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">中找到答案。希望所有的朋友都能够带着这些问题阅读本书，学以致</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">用。</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">请大家注意，每个问题后面都有</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000"> </span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">的数量表示问题的难</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">度——数量越多，难度越大。</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">关于推理大模型</span>

<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">Q1：根据缩放定律，如何估算训练一个特定规模的大模型所需的预训</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">练数据集的大小和算力？</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000"> </span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">Q2：通过let&#x27;s</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">think</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">step</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">by</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">step提示词触发的思维链模式，与推理</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">模型的原理有什么不同？同样是测试时计算，为什么推理模型的上限</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">更高？</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000"> </span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">Q3：如果需要针对垂直领域微调推理模型，PRM（过程奖励模型）</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">和ORM（结果奖励模型）分别适合什么场景？</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000"> </span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">Q4：在MCTS（蒙特卡洛树搜索）方法中，如何平衡探索和利用？探</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">索和利用分别使用什么方式来评估？</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000"> </span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">Q5：STaR（自我教导推理器）方法是如何让模型通过自我生成的推</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">理数据来改进自身的？它有什么优缺点？</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000"> </span>

<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">Q6：对于每个输出词元的成本，为什么推理模型一般高于架构和参数</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">量相同的非推理模型？</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000"> </span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000"> </span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000"> </span>

<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">标注，其中</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000"> </span>

![](./图解DeepSeek技术_assets/images/p005_img01.jpg)



![](./图解DeepSeek技术_assets/images/p005_img02.jpg)



![](./图解DeepSeek技术_assets/images/p005_img03.jpg)



![](./图解DeepSeek技术_assets/images/p005_img04.jpg)



![](./图解DeepSeek技术_assets/images/p005_img05.jpg)



![](./图解DeepSeek技术_assets/images/p005_img06.jpg)



![](./图解DeepSeek技术_assets/images/p005_img07.jpg)



![](./图解DeepSeek技术_assets/images/p005_img08.jpg)



![](./图解DeepSeek技术_assets/images/p005_img09.jpg)



![](./图解DeepSeek技术_assets/images/p005_img10.jpg)



![](./图解DeepSeek技术_assets/images/p005_img11.jpg)



![](./图解DeepSeek技术_assets/images/p005_img12.jpg)



![](./图解DeepSeek技术_assets/images/p005_img13.jpg)

---

## Page 6

<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">Q7：在后训练过程中，推理模型的思维链会越来越长，这样会提高结</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">果的准确率，但也增加了响应延迟。请问，如何根据问题复杂度、用</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">户需求和系统负载自动调整推理深度？</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000"> </span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000"> </span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">Q8：在实时语音对话应用中，如何利用推理模型提升性能，同时避免</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">过高的响应延迟影响用户体验？</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000"> </span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000"> </span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000"> </span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">Q9：如何用RL（强化学习）方法提升一个大模型的工具调用能力？</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">如何训练模型，使其能够智能地决定何时依靠内部推理能力，何时调</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">用外部工具（例如写一段代码来解决复杂的推理问题，而不是在输出</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">的推理过程中穷举所有可能）？</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000"> </span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">关于DeepSeek-R1</span>

<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">Q10：DeepSeek-R1与DeepSeek-R1-Zero的训练过程有什么区别，</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">各自有什么优缺点？既然DeepSeek-R1-Zero生成的推理过程可读性</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">差，在非推理任务上的表现也不如DeepSeek-R1，那么，DeepSeek-</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">R1-Zero存在的价值是什么？DeepSeek-R1的训练过程是如何解决</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">DeepSeek-R1-Zero的上述问题的？</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000"> </span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">Q11：DeepSeek-R1为什么没有使用PRM、MCTS、束搜索等方法？</span>

![](./图解DeepSeek技术_assets/images/p006_img01.jpg)



![](./图解DeepSeek技术_assets/images/p006_img02.jpg)



![](./图解DeepSeek技术_assets/images/p006_img03.jpg)



![](./图解DeepSeek技术_assets/images/p006_img04.jpg)



![](./图解DeepSeek技术_assets/images/p006_img05.jpg)



![](./图解DeepSeek技术_assets/images/p006_img06.jpg)



![](./图解DeepSeek技术_assets/images/p006_img07.jpg)



![](./图解DeepSeek技术_assets/images/p006_img08.jpg)



![](./图解DeepSeek技术_assets/images/p006_img09.jpg)



![](./图解DeepSeek技术_assets/images/p006_img10.jpg)



![](./图解DeepSeek技术_assets/images/p006_img11.jpg)



![](./图解DeepSeek技术_assets/images/p006_img12.jpg)

---

## Page 7

<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">Q12：DeepSeek-R1使用的GRPO（组相对策略优化）与PPO（近端</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">策略优化）有什么区别？为什么GRPO不需要评论家模型？</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000"> </span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">Q13：DeepSeek-R1在SFT（监督微调）阶段，为什么要加入
20万条</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">与推理无关的训练样本？</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000"> </span>

<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">Q14：DeepSeek是如何把DeepSeek-R1的推理能力蒸馏到较小的模</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">型中的？如果我们要自己蒸馏一个较小的垂直领域模型，如何尽可能</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">保留DeepSeek-R1在特定领域的能力？</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000"> </span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">Q15：事实上，MLA（多头潜在注意力）相比MQA（多查询注意</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">力）占用的KV（键</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">-</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">值）缓存更多，为什么MLA在性能上优于</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">MQA？MLA对哪个维度做了低秩压缩？</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000"> </span>

<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">Q16：MLA是如何解决RoPE（旋转位置编码）与低秩KV不兼容问题</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">的？如果采用其他基于注意力偏置的位置编码，会有什么问题？</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000"> </span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">Q17：为什么DeepSeekMoE模型前
3层采用稠密连接而后续采用MoE</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">（混合专家）？如果所有层都使用MoE，会有什么影响？</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000"> </span>

<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">Q18：DeepSeekMoE和Mixtral</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">MoE有什么区别？DeepSeekMoE</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">的细粒度专家分割和共享专家隔离有什么优点？</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000"> </span>

<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">Q19：DeepSeekMoE中的专家负载均衡是如何解决路由崩溃问题</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">的？</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000"> </span>

![](./图解DeepSeek技术_assets/images/p007_img01.jpg)



![](./图解DeepSeek技术_assets/images/p007_img02.jpg)



![](./图解DeepSeek技术_assets/images/p007_img03.jpg)



![](./图解DeepSeek技术_assets/images/p007_img04.jpg)



![](./图解DeepSeek技术_assets/images/p007_img05.jpg)



![](./图解DeepSeek技术_assets/images/p007_img06.jpg)



![](./图解DeepSeek技术_assets/images/p007_img07.jpg)



![](./图解DeepSeek技术_assets/images/p007_img08.jpg)



![](./图解DeepSeek技术_assets/images/p007_img09.jpg)



![](./图解DeepSeek技术_assets/images/p007_img10.jpg)



![](./图解DeepSeek技术_assets/images/p007_img11.jpg)



![](./图解DeepSeek技术_assets/images/p007_img12.jpg)

---

## Page 8

<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">Q20：相比一次预测一个词元，DeepSeek-V3的多词元预测方法在样</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">本利用效率和推理效率方面有什么优势？</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000"> </span>

<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">Q21：DeepSeek-V3的混合精度训练在哪些矩阵计算中使用了FP8量</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">化？为了减少对模型精度的影响，DeepSeek-V3是如何对激活值和权</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">重做分组量化的？</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000"> </span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000"> </span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">Q22：DeepSeek-R1-Zero的方法主要适用于有明确验证机制的任务</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">（如数学、编程），如何将这一方法扩展到更主观的领域（如创意写</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">作或战略分析）？</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000"> </span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000"> </span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">Q23：如果要在一个非推理模型的基础上，通过强化学习训练出一个</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">在1000以内的整数的四则运算中错误率低于1%的模型，预计基座模</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">型的参数规模至少需要多大，训练过程需要配备多少块GPU，训练时</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">长是多少？（提示：TinyZero）</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000"> </span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000"> </span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000"> </span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">Q24：在QwQ-32B这类推理模型的基础上，如何通过强化学习在类似</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">OpenAI</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">Deep</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">Research的场景中进一步增强其深度搜索能力？</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000"> </span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">最后，感谢我的搭档孟佳颖，本书是我们一起完成的第一部译作。如</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">有错漏或不当之处，恳请读者指正。</span>

<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">李博杰、孟佳颖</span>

![](./图解DeepSeek技术_assets/images/p008_img01.jpg)



![](./图解DeepSeek技术_assets/images/p008_img02.jpg)



![](./图解DeepSeek技术_assets/images/p008_img03.jpg)



![](./图解DeepSeek技术_assets/images/p008_img04.jpg)



![](./图解DeepSeek技术_assets/images/p008_img05.jpg)



![](./图解DeepSeek技术_assets/images/p008_img06.jpg)



![](./图解DeepSeek技术_assets/images/p008_img07.jpg)



![](./图解DeepSeek技术_assets/images/p008_img08.jpg)



![](./图解DeepSeek技术_assets/images/p008_img09.jpg)



![](./图解DeepSeek技术_assets/images/p008_img10.jpg)



![](./图解DeepSeek技术_assets/images/p008_img11.jpg)



![](./图解DeepSeek技术_assets/images/p008_img12.jpg)



![](./图解DeepSeek技术_assets/images/p008_img13.jpg)



![](./图解DeepSeek技术_assets/images/p008_img14.jpg)

---

## Page 9

2025年4月

<!-- 图源: ./图解DeepSeek技术_assets/images/p009_scan.jpg -->


<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">2025年4月</span>

---

## Page 10

<span style="font-family:'Unnamed-T3';font-size:21.25pt;color:#000000">前言</span>

<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">近五年来，大语言模型</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000"> </span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">后续简称为大模型或者LLM。——编者注</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">（Large</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">Language</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">Model，LLM）异军突起，已然成为AI历史上举足轻重的</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">模型之一。2022年
11月的“ChatGPT时刻”在全球引发了巨大反响。</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">一个软件系统竟能就各种话题进行流畅的对话，其水平之高前所未</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">见，这一“巨震”推动了ChatGPT用户量的爆炸式增长——短短两个</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">月内就吸引了超过一亿用户。</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">随后的几年里，该领域及大模型的能力都取得了显著进展，其中一个</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">里程碑事件便是2025年
1月的“DeepSeek时刻”：在发布了一系列高</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">质量模型之后，来自浙江杭州的DeepSeek团队推出了DeepSeek-</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">R1。DeepSeek-R1是首个在质量上可与早几周由OpenAI发布的o1推</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">理模型相媲美的开源推理大模型。一些人认为DeepSeek-R1的发布引</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">发了美国股市历史上最大的震荡之一，导致数千亿美元市值蒸发，投</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">资者开始重新审视那些站在AI浪潮前沿的科技巨头的估值。</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">然而，推理大模型究竟是什么？它们是如何工作的，又是如何训练</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">的？</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">本书将以DeepSeek-R1为例深入探讨这些问题，并提供更多解答。我</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">们将为读者展示如何直观理解推理大模型的若干核心概念。为了让尽</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">可能多的读者理解这些概念，我们大量运用图示，力求以最清晰、友</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">好的方式阐释相关概念。基于多年来向数百万读者讲解复杂AI概念的</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">经验，我们逐渐形成了一套成熟的视觉语言和叙事方法。本书同样基</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">于这套方法精心设计，引导读者先聚焦最重要的思想，然后循序渐进</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">地构建更完整的知识图景，从而逐步加深对该主题的理解并增强掌握</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">相关知识的信心。</span>

![](./图解DeepSeek技术_assets/images/p010_img01.png)

---

## Page 11

<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">读完本书，你将掌握诸如测试时计算（test-time</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">compute）、构成</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">DeepSeek-R1模型的Transformer架构，以及包括GRPO算法在内的</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">推理大模型训练方案等。最后，我们将一同回顾DeepSeek在其“开</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">源周”期间公布的代码——该公司开源了五个代码库，其中包含了支</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">撑其在线服务的核心系统。</span>

---

## Page 12

<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">第1章</span>
<span style="font-family:'Unnamed-T3';font-size:21.25pt;color:#000000">测试时计算</span>

<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">DeepSeek-R1、OpenAI</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">o3-mini与Google</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">Gemini</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">2.0</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">Flash</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">Thinking是将大模型推向新高度的代表性成果，它们背后的“推理”</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">框架正是实现这一突破的关键。这些模型标志着范式的转变——从扩</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">展训练时计算（train-time</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">compute）转向扩展测试时计算</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000"> </span>

<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">测试时计</span>

<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">算的另外一种常见的叫法是“推理时计算”。——编者注</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">（test-time</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">compute）。</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">在本章中，我们将深入探讨这一范式转变，并系统介绍构建具备推理</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">能力的大模型所依赖的核心方法。在此之前，我们需要先回答一个基</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">础问题：什么是推理大模型（reasoning</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">LLM）？</span>

![](./图解DeepSeek技术_assets/images/p012_img01.png)

---

## Page 13

<span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000">1.1</span><span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000">什么是推理大模型</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">与常规大模型相比，推理大模型倾向于将问题分解为更小的步骤（通</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">常称为推理步骤或思考过程），再作出回答。两者的底层逻辑如图1-1</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">所示。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">那么，“思考过程”“推理步骤”或“思维链”（Chain-of-</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">Thought）究竟意味着什么？</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">尽管我们可以从哲学角度探讨大模型是否真正具备类人思维能力</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">理学视角观察，大模型展现出的“深思熟虑”特质令人惊叹。但需要注意，这些“推理”步骤</span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">可能会过度模仿人类的行为模式。如果我们改用符号语言，大模型的“推理”过程会是什么样</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图1-1</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">常规大模型与推理大模型</span>

<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">从心</span>

![](./图解DeepSeek技术_assets/images/p013_img01.jpg)



![](./图解DeepSeek技术_assets/images/p013_img02.png)

---

## Page 14

<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">子呢？</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">，但这些推理步骤的本质是将处理过程拆解为更小、结构化程度</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">更高的推理单元，如图1-2所示。</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图1-2</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">推理大模型的推理步骤</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">换言之，这类模型不再局限于学习“回答什么”，而是学习“如何回</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">答”！</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">要理解推理大模型的构建原理，我们首先需要探究从扩展训练（训练</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">时计算）到扩展推理（测试时计算）的范式转变。</span>

![](./图解DeepSeek技术_assets/images/p014_img01.jpg)

---

## Page 15

<span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000">1.2</span><span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000">什么是训练时计算</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">截至2024年6月，开发者通常通过扩大以下要素的规模来提升大模型在</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">预训练阶段的性能：</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">参数量（模型规模）</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">数据量（数据集大小，也称数据集规模，即词元量</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">训练数据集的词元数量。——译者注</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">）</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">计算量（浮点运算量，FLOPs）</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">这三个要素如图1-3所示。三者共同决定了模型的训练时计算量，即完</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">成预训练所需的总计算资源。人们常将预训练数据比作AI的“化石燃</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">料”——这就意味着预训练投入越大，最终模型的能力通常会越强。</span>

<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">这里的词元量指的是预</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图1-3</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">提升大模型预训练性能的关键要素</span>

![](./图解DeepSeek技术_assets/images/p015_img01.png)



![](./图解DeepSeek技术_assets/images/p015_img02.jpg)

---

## Page 16

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">训练时计算可能同时涵盖预训练阶段与微调阶段的需求，如图1-4所</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">示。</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">这些要素共同成为提升大模型性能的主要关注点。</span>
<span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000">缩放定律</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">研究者通过多种缩放定律（scaling</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">law）来探索三个要素的规模（计</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">算量、数据量和参数量）与模型性能之间的关联。这类定律本质上是</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">“幂律”</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">的某个幂次成正比。在图1-5中，当坐标系中的两个轴是对数刻度时，图像呈现为一条直线。</span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">——译者注</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">（power</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">law）——当某个变量（例如计算量）增加时，另一</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">个变量（例如模型性能）会产生相应比例的变化。在线性坐标系和双</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">对数坐标系中，模型性能和计算量之间的关系如图1-5所示。</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图1-4</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">训练时计算</span>

<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">幂律指的是两个变量之间存在一种特定的关系，即其中一个变量与另一个变量</span>

![](./图解DeepSeek技术_assets/images/p016_img01.jpg)



![](./图解DeepSeek技术_assets/images/p016_img02.png)

---

## Page 17

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图1-5</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">模型性能和计算量之间的关系：线性坐标系和双对数坐标系</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">缩放定律通常显示在双对数坐标系中，结果呈现为一条直线，用以直</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">观展示计算量的大幅增长对模型性能的影响。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">最广为人知的缩放定律是Kaplan缩放定律</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Laws</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">for</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Neural</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Language</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Models.”arXiv</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">preprint</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">arXiv:2001.08361</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">(2020).</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">和</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">Chinchilla缩放定律</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Compute-optimal</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Large</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Language</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Models.”arXiv</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">preprint</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">arXiv:2203.15556</span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">(2022).</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">。这类定律大致表明：随着计算量、数据量和参数量的增加，模</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">型性能将呈现上升趋势。图1-6展示了缩放定律的核心原理——在合理</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">配置训练词元与模型参数比例的前提下，通过扩大模型规模可有效提</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">升预测准确率。</span>

<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Kaplan,</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Jared,</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">et</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">al.“Scaling</span>

<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Hoffmann,</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">J.,</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Borgeaud,</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">S.,</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Mensch,</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">A.,</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">et</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">al.“Training</span>

![](./图解DeepSeek技术_assets/images/p017_img01.jpg)



![](./图解DeepSeek技术_assets/images/p017_img02.png)



![](./图解DeepSeek技术_assets/images/p017_img03.png)

---

## Page 18

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">该注释图来自“Scaling</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">Laws</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">for</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">Neural</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">Language</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">Models”论</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">文。图中展示了模型性能如何随着不同的计算指标（计算量、数据集</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">大小和模型规模）的增加而提升。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图1-6</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">缩放定律的核心原理</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">研究指出，这三个要素必须同步扩展才能获得最佳性能。当其他两个</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">要素不构成瓶颈时，性能与其中任一单独要素之间呈幂律关系。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">Kaplan缩放定律认为：在固定计算量的前提下，扩大模型规模通常比</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">增加数据量更有效。与之相反，Chinchilla缩放定律则主张模型规模</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">与数据量同等重要。</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">算量、数据集大小和参数规模的最优配比服从幂律分布，最优参数规模比最优数据集大小增长</span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">更快；在固定的训练算力预算下，训练参数量更大的模型并在收敛前停止，可以得到最佳性</span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">能。Chinchilla缩放定律由DeepMind于2022年提出，其主要结论为：在固定的训练算力预</span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">算下，模型参数规模和数据集大小应该等比例缩放；对于给定参数量的模型，最佳的训练数据</span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">集大小约为模型中参数量的20倍（像GPT-3这样的模型参数量过大，训练不足）。——译者注</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">然而纵观2024年，虽然计算量、数据集大小和模型参数量都在持续增</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">长，但性能提升呈现出边际收益递减的趋势，如图1-7所示</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">模型性能随计算量增长的提升效果。——编者注</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">。</span>

<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Kaplan缩放定律由OpenAI于2020年提出，其主要结论为：计</span>

<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">图中只展示了</span>

![](./图解DeepSeek技术_assets/images/p018_img01.jpg)



![](./图解DeepSeek技术_assets/images/p018_img02.png)



![](./图解DeepSeek技术_assets/images/p018_img03.png)

---

## Page 19

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图1-7</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">模型性能提升的边际收益递减</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">这正是幂律的一个特点：随着规模的不断扩大，增益逐渐减小。这引</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">出了一个关键问题：</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">“我们是否遇到了瓶颈？”</span>

![](./图解DeepSeek技术_assets/images/p019_img01.jpg)

---

## Page 20

<span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000">1.3</span><span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000">什么是测试时计算</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">由于提升训练时计算的成本过高，研究人员开始关注另一种研究方向</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">——测试时</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">于测试时。——译者注</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">计算。不同于持续增加预训练预算的做法，测试时计</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">算关注的是确保模型能够在推理（inference）过程中进行“长时间思</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">考”，如图1-8所示。</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图1-8</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">测试时计算关注模型推理过程中的“长时间思考”</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">非推理模型通常会跳过所有“推理”步骤，直接输出答案，如图1-9所</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">示。</span>

<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">测试时指的是在模型训练完成后使用模型的阶段，例如在线使用ChatGPT属</span>

![](./图解DeepSeek技术_assets/images/p020_img01.png)



![](./图解DeepSeek技术_assets/images/p020_img02.jpg)

---

## Page 21

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图1-9</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">非推理模型直接输出答案</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">然而，推理模型则会通过系统化的思考过程消耗更多词元来推导答</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">案，如图1-1
0所示。</span>

![](./图解DeepSeek技术_assets/images/p021_img01.jpg)

---

## Page 22

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图1-10</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">推理模型消耗更多词元推导答案</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">该理念的核心在于：大模型需要消耗资源（如显存、算力）来生成答</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">案。然而，若将所有计算资源都用于直接生成最终答案，这种处理方</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">式在效率上略显不足！</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">通过预先生成更多包含附加信息、关联关系及新思路的词元，模型能</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">够将更多算力投入到最终答案的生成过程中，如图1-1
1所示。</span>

![](./图解DeepSeek技术_assets/images/p022_img01.jpg)

---

## Page 23

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图1-11</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">“提前计算”机制</span>

<span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000">1.3.1</span><span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000">缩放定律</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">与训练时计算相比，测试时计算的缩放定律是相对较新的研究领域。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">值得注意的是，两个有趣的来源将测试时计算的扩展与训练时计算的</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">扩展联系起来。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">首先，OpenAI的一篇博客文章“Learning</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">to</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">reason</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">with</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">LLMs”</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">指出，测试时计算可能实际上与训练时计算遵循相似的缩放定律，如</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图1-1
2所示。</span>

![](./图解DeepSeek技术_assets/images/p023_img01.jpg)

---

## Page 24

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">该注释图来自论文“Learning</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">to</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">reason</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">with</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">LLMs”，其中的红色</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">虚线为笔者添加，用于展示OpenAI可能发现了一种新范式——测试时</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">计。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图1-12</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">训练时计算与测试时计算可能遵循相似的缩放定律</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">练时计算量的对数大致成正比，与测试时计算量的对数也大致成正比；且在总算力预算固定的</span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">情况下，测试时计算量的增加比训练时计算量的增加对模型性能的提升可能更有效。——译者</span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">注</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">基于此，研究人员提出可能会出现一种范式转变，即从扩展训练时计</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">算转向扩展测试时计算。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">其次，一篇名为“Scaling</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">Scaling</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">Laws</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">with</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">Board</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">Games”</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Jones,</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Andy</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">L.“Scaling</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Scaling</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Laws</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">with</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Board</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Games.”arXiv</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">preprint</span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">arXiv:2104.03113</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">(2021).</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">的论文通过AlphaZero系统进行了有趣的探索：研</span>

<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">模型性能与训</span>

![](./图解DeepSeek技术_assets/images/p024_img01.jpg)



![](./图解DeepSeek技术_assets/images/p024_img02.png)



![](./图解DeepSeek技术_assets/images/p024_img03.png)

---

## Page 25

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">究人员训练不同计算量配置的AlphaZero来玩六边形棋盘（Hex）游</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">戏，如图1-1
3所示。</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">该注释图来自论文“Scaling</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">Scaling</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">Laws</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">with</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">Board</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">Games”。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图1-13</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">构建不同计算量配置的训练时计算与测试时计算</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">他们的研究结果表明，训练时计算与测试时计算存在紧密关联，如图1-</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">14所示。图中虚线表示达到特定ELO分数所需的最小计算量。</span>

![](./图解DeepSeek技术_assets/images/p025_img01.jpg)

---

## Page 26

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">该注释图来自论文“Scaling</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">Scaling</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">Laws</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">with</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">Board</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">Games”。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图1-14</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">训练时计算与测试时计算的关联关系</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">当测试时计算像训练时计算一样持续扩展时，一场范式转变即将发生</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">——通过增加测试时计算量的方式构建“推理”模型。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">通过这种范式转变，这些“推理”模型不再单纯聚焦于训练时计算</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">（预训练与微调</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">训练，且后训练所需的算力往往比传统的监督微调大很多。——译者注</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">），而是协同平</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">衡训练过程与推理过程，如图1-1
5所示。</span>

<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">此处的“微调”并不单指监督微调（SFT），更重要的是强化学习后</span>

![](./图解DeepSeek技术_assets/images/p026_img01.jpg)



![](./图解DeepSeek技术_assets/images/p026_img02.png)

---

## Page 27

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">测试时计算的扩展甚至延伸至序列长度的增加，如图1-1
6所示。</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图1-16</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">测试时计算的扩展可能与序列长度成正比</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">时间长度来类比思考词元的序列长度。大模型的实际推理时间一般在数秒到数分钟之间，每秒</span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">输出几十到几百个思考词元，因此思考过程可以输出几十到几万个不等的思考词元。输出的思</span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">考词元的数量跟问题难度有关，问题难度越大，思考词元的序列越长。——译者注</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">我们将在深入探讨DeepSeek-R1时进一步探索扩展序列长度的可能</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">性！</span>
<span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000">1.3.2</span><span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000">测试时计算的分类</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图1-15</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">协同平衡训练过程与推理过程</span>

<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">原作者是用天、周、月的</span>

![](./图解DeepSeek技术_assets/images/p027_img01.jpg)



![](./图解DeepSeek技术_assets/images/p027_img02.jpg)



![](./图解DeepSeek技术_assets/images/p027_img03.png)

---

## Page 28

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">DeepSeek-R1与OpenAI</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">o1等推理模型取得的惊人成功表明，提升模</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">型性能的技术远不止简单的“延长思考时间”这一种路径。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">正如我们将要探讨的，测试时计算可以体现为多种不同的形式，包括</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">思维链（Chain-of-Thought）、答案修正、回溯推理、采样优化等。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">这些方法大致可分为两类</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Compute</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Optimally</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">can</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">be</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">More</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Effective</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">than</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Scaling</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Model</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Parameters.”arXiv</span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">preprint</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">arXiv:2408.03314</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">(2024).</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">：</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">基于验证器（verifier）的搜索（通过采样生成多个候选答案并选择最</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">优解）</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">调整提议分布</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">分布可以让模型以更大的概率输出更好的思考过程和正确答案。——译者注</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">（proposal</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">distribution）（经过训练的“思考”过程）</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">这两类方法如图1-1
7所示。</span>

<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Snell,</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Charlie,</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">et</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">al.“Scaling</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">LLM</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Test-Time</span>

<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">提议分布指的是推理过程中候选答案或思考过程的概率分布，调整提议</span>

![](./图解DeepSeek技术_assets/images/p028_img01.png)



![](./图解DeepSeek技术_assets/images/p028_img02.png)

---

## Page 29

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图1-17</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">测试时计算的两类方法</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">输出思考过程，这里为简化图示而省略了。图中的RM指的是奖励模型（reward</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">model），</span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">是强化学习中用于评估智能体行为并提供反馈信号的函数。经过训练的奖励模型可以为智能体</span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">的决策提供指导，优化智能体的行为。——译者注</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">因此，基于验证器的搜索是输出导向的，而调整提议分布的方法则是</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">输入导向的，如图1-1
8所示。</span>

<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">在基于验证器的搜索方法中，生成答案之前也需要</span>

![](./图解DeepSeek技术_assets/images/p029_img01.jpg)



![](./图解DeepSeek技术_assets/images/p029_img02.png)

---

## Page 30

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图1-18</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">输出导向与输入导向</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">我们将探讨以下两种验证机制：</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">结果奖励模型（Outcome</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">Reward</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">Model，ORM）</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">过程奖励模型（Process</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">Reward</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">Model，PRM）</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">顾名思义，ORM仅对最终结果进行评估，并不关注底层过程，如图1-</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">19所示。</span>

![](./图解DeepSeek技术_assets/images/p030_img01.jpg)

---

## Page 31

问题

LLM
思考过程1
推理步骤
思考过程2

思考过程n
答案  ORM0.8
仅评估最终答案

<!-- 图源: ./图解DeepSeek技术_assets/images/p031_scan.jpg -->


<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图1-19</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">ORM（结果奖励模型）</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">相比之下，PRM还会对导致结果的整个过程（即“推理”）进行评</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">估，如图1-2
0所示。</span>

---

## Page 32

问题

LLM
思考过程1  PRM  0.2
推理步骤
思考过程2  PRM  0.8

思考过程n  PRM  0.1
最终答案
(不计分)  答案

<!-- 图源: ./图解DeepSeek技术_assets/images/p032_scan.jpg -->


<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图1-20</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">PRM（过程奖励模型）</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">为了更清楚地了解PRM对推理步骤评分的过程，我们具体看一个例</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">子，如图1-2
1所示。</span>

---

## Page 33

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图1-21</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">PRM针对具体问题的验证过程</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">请注意，第
2步的推理质量较差，因此被PRM评分较低！</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">在理解了ORM与PRM的核心区别后，接下来我们探索一下两者在不同</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">验证技术中的应用方法。</span>

![](./图解DeepSeek技术_assets/images/p033_img01.jpg)

---

## Page 34

<span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000">1.4</span><span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000">基于验证器的搜索</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">我们来看第一种测试时计算——基于验证器的搜索，该方法</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">的“并行生成多个候选样本并让验证器挑选”的实现步骤，还有一种基于验证器的实现是顺序</span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">修正（sequential</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">revision），即由验证器在候选样本的基础上进行修改，或是提出修改意</span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">见，交给大模型重新生成回答。——译者注</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">通常包含两个步骤：</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">首先，生成多个推理过程和答案的候选样本；</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">其次，通过验证器（奖励模型）对生成的输出进行评分。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">基于验证器的搜索的两个步骤如图1-2
2所示。</span>

<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">除了这里讲</span>

![](./图解DeepSeek技术_assets/images/p034_img01.png)

---

## Page 35

问题

LLM

思考  思考  思考  思考
生成
思考和答案  答案  答案  答案  答案

RM  RM  RM  RM
验证
思考/答案

选择
最佳答案

<!-- 图源: ./图解DeepSeek技术_assets/images/p035_scan.jpg -->


<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图1-22</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">基于验证器的搜索的两个步骤</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">验证器通常采用大模型，经过微调专门用于评判结果（ORM）或推理</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">过程（PRM）。</span>

---

## Page 36

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">使用验证器的主要优势在于：不需要对用于问题解答的大模型进行重</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">新训练或微调。</span>
<span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000">1.4.1</span><span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000">多数投票法</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">最直接的方法其实并不需要奖励模型或验证器，而是通过多数投票机</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">制实现。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">具体操作是让模型生成多个答案，最终选取出现频次最高的答案作为</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">最终解，如图1-2
3所示。</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">这种方法也被称为自一致性（self-consistency</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">“Self-Consistency</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Improves</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Chain</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">of</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Thought</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Reasoning</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">in</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Language</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Models.”</span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">arXiv</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">preprint</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">arXiv:2203.11171</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">(2022).</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">），以强调生成多个答案和推理步骤的</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">必要性。</span>
<span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000">1.4.2</span><span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000">Best-of-N采样</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图1-23</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">多数投票机制</span>

<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Wang,</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Xuezhi,</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">et</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">al.</span>

![](./图解DeepSeek技术_assets/images/p036_img01.jpg)



![](./图解DeepSeek技术_assets/images/p036_img02.png)

---

## Page 37

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">第一种涉及验证器的方法称为Best-of-N采样。该技术生成N个样本，</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">随后使用验证器（如结果奖励模型）对每个答案进行评估。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">首先，大模型（通常称为提议者，Proposer）通过设置较高的温度</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">温度（temperature）是指大模型在生成每个词元时采样的随机程度。温度值设置为
0代表大</span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">模型总是选取概率最高的词元，在这种情况下，大模型的输出一般是确定的。温度值越高，输</span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">出的随机程度越高、越多样化。——译者注</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">或变化的温度参数生成多个答案，如</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图1-2
4所示。</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图1-24</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">LLM生成多个答案</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">其次，每个答案都会经过结果奖励模型（ORM）的处理，根据答案的</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">质量进行评分。最终选择得分最高的答案，如图1-2
5所示。</span>

![](./图解DeepSeek技术_assets/images/p037_img01.png)



![](./图解DeepSeek技术_assets/images/p037_img02.jpg)

---

## Page 38

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图1-25</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">选择得分最高的答案作为最终答案</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">相较于单纯评估最终答案，推理过程也可以通过过程奖励模型进行评</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">估，该模型会对每个推理步骤的质量进行评判，并选择总权重最高的</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">候选推理路径，如图1-2
6所示。</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图1-26</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">选择总权重最高的候选推理路径</span>

![](./图解DeepSeek技术_assets/images/p038_img01.jpg)



![](./图解DeepSeek技术_assets/images/p038_img02.jpg)

---

## Page 39

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">对于这两种验证器类型，我们还可以通过奖励模型对每个候选答案进</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">行加权，并选择总权重最高的答案</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">过程分别使用过程奖励模型打分，最后选取总分最高的候选答案。——译者注</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">。这种方法</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">被称为加权Best-of-N采样，如图1-2
7所示。</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图1-27</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">加权Best-of-N采样</span>

<span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000">1.4.3</span><span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000">基于过程奖励模型的束搜索</span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">思维树框架在每一步都会提出多个候选的下一步思考过程，并通过过程奖励模型评估得到最</span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">优的几个候选思考过程，然后分别在其基础上扩展下一步思考。——译者注</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">通过束搜索（beam</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">search）可以进一步扩展生成答案与中间步骤的</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">过程。该方法会采样多条推理路径，并经由过程奖励模型对每条路径</span>

<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">每个候选答案生成多个推理过程，每个推理</span>

![](./图解DeepSeek技术_assets/images/p039_img01.png)



![](./图解DeepSeek技术_assets/images/p039_img02.jpg)



![](./图解DeepSeek技术_assets/images/p039_img03.png)

---

## Page 40

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">进行评分（类似思维树框架</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Problem</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Solving</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">with</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Large</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Language</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Models.”Advances</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">in</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Neural</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Information</span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Processing</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Systems</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">36</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">(2024).</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">）。系统会持续追踪整个推理过程中评分最高</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">的3个“束”（最佳路径），如图1-2
8所示。</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">该方法能够快速终止未能产生有效结果的“推理”路径（PRM给出的</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">评分较低），最终我们将采用先前探讨的Best-of-N对结果答案进行加</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">权。</span>
<span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000">1.4.4 蒙特卡洛树搜索</span>

<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Yao,</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Shunyu,</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">et</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">al.“Tree</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">of</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Thoughts:</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Deliberate</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图1-28</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">基于过程奖励模型的束搜索</span>

![](./图解DeepSeek技术_assets/images/p040_img01.png)



![](./图解DeepSeek技术_assets/images/p040_img02.jpg)

---

## Page 41

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">蒙特卡洛树搜索（Monte</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">Carlo</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">Tree</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">Search，MCTS）是提升树搜索</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">效率的重要技术，其执行流程包含四个核心步骤。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">选择（selection）：基于预设策略选择特定的叶节点。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">扩展（expand）：创建额外的节点。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">模拟（rollouts）：随机生成新节点直至终局。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">回传（backprop）：根据输出结果更新父节点的评分。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">该流程旨在持续扩展最佳推理路径的同时兼顾其他路径的探索，本质</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">上实现了利用（exploitation）与探索（exploration）的动态平衡。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">以下是对节点进行选择和评分的典型示例，如图1-2
9所示。</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图1-29</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">对节点进行选择和评分</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">[1]</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">因此，当我们选择一个新的推理步骤进行探索时，该步骤并不一定是</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">迄今为止性能最佳的路径。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">采用这类方法时，我们首先选择一个节点（即推理步骤），并通过生</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">成新的推理步骤来扩展它，如图1-3
0所示。与之前一样，这一过程可</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">以通过设置较高且多样化的温度值来实现。</span>

![](./图解DeepSeek技术_assets/images/p041_img01.jpg)

---

## Page 42

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">接下来从已扩展的推理步骤中选择一个节点，通过多次路径模拟直至</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">获得多个答案。这些模拟路径可以根据推理步骤的质量（PRM）、奖</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">励（ORM）或二者的综合评估进行判断。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">完成评估后，通过回传</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">backprop），但回传机制是在推理过程中使用的，指的是更新蒙特卡洛树搜索中父节点的评</span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">分；而反向传播机制是在训练过程中使用的。——译者注</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">机制更新父节点的评分，继</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">而重新启动从节点选择开始的完整推理流程。模拟和回传的过程如图1-</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">31所示。</span>

<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">回传与反向传播的英文都是backpropagation（简写为</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">▲图1-30</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">选择与扩展</span>

![](./图解DeepSeek技术_assets/images/p042_img01.png)



![](./图解DeepSeek技术_assets/images/p042_img02.jpg)

---

## Page 43

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">▲图1-31</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">模拟与回传</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">[1]每个节点表示候选思考过程中的一个步骤。左侧的“利用项”表示</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">节点的平均回报，鼓励优先选择当前看起来得分更高的路径。右侧的</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">“探索项”在标准UCB1公式中通常写为：</span>

![](./图解DeepSeek技术_assets/images/p043_img01.jpg)



![](./图解DeepSeek技术_assets/images/p043_img02.jpg)

ln（父节点访问次数）
当前节点访问次数

---

## Page 44

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">用于提升那些访问次数较少的节点得分，从而鼓励探索新的路径。其</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">中，分子中的对数函数可以避免随着总访问次数增加导致探索项过度</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">膨胀，使搜索策略逐步集中到更有希望的路径上。——译者注</span>

---

## Page 45

<span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000">1.5</span><span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000">调整提议分布</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">构建推理大模型的第二类方法被称为“调整提议分布”。与基于验证</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">器搜索正确推理步骤（“输出导向”）的方法不同，后者通过训练模</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">型直接生成改进后的推理步骤（“输入导向”）。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">具体而言，该方法通过调整补全结果/思考过程/词元的采样分布来实现</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">优化。假设我们面对一个问题，并拥有一个可从中采样词元的分布。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">常规策略通常是选取得分最高的词元，如图1-3
2所示。</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图1-32</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">选择词元的常规策略：贪婪词元</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">然而需要注意的是，图1-3
2中的部分词元被标记为红色。这些词元引</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">发推理过程的可能性更大，如图1-3
3所示。</span>

![](./图解DeepSeek技术_assets/images/p045_img01.jpg)

---

## Page 46

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图1-33</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">标记为红色的词元的推理过程</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">虽然选择贪婪词元本身未必错误，但优先选取能够引导推理过程的词</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">元往往可以生成更优质的答案。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">调整提议分布（又称词元概率分布）的本质是让模型对该分布进行重</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">新排序，从而使“推理”类词元获得更高的选择优先级，如图1-34所</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">示。</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">提议分布是指模型输出的候选词元的概率分布。在图1-3
4的例子中，模型可能本来倾</span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">向于直接输出答案（即输出3、4、5的概率更高），但直接输出的答案未经深入思考，正确率</span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">不高。调整提议分布就是让模型生成的第一个词元更有可能是推理句子的起始词元，这样模型</span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">在生成后续词元时就会尝试补全这个句子，从而触发思考过程。类似地，通过训练模型在准备</span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">输出答案前先输出But、Wait等词元，可以触发模型反思之前的推理过程。再比如，通过训练</span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">模型在遇到复杂的数学计算时，先输出一个工具调用的起始词元，可以触发模型调用计算器来</span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">计算数学表达式，提升计算准确率。除了计算器，模型还可以在推理过程中调用搜索、代码执</span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">行等多种工具，从而大大提升模型解决实际问题的能力。——译者注</span>

![](./图解DeepSeek技术_assets/images/p046_img01.jpg)



![](./图解DeepSeek技术_assets/images/p046_img02.png)

---

## Page 47

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图1-34</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">调整提议分布</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">调整提议分布的方法有多种，但总体上可分为两类：</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">通过提示工程优化提示词；</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">训练模型以专注于推理词元/推理过程。</span>
<span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000">1.5.1</span><span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000">提示工程</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">提示工程（prompt</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">engineering）的方法允许我们通过优化提示词来</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">改进模型的输出质量。这种方法还可能促使模型展现我们此前观察到</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">的某些推理过程。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">若要通过提示词调整提议分布，我们可以为模型提供需要遵循的示例</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">（上下文学习），从而使其生成类推理行为，如图1-3
5所示。</span>

![](./图解DeepSeek技术_assets/images/p047_img01.jpg)

---

## Page 48

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">通过简单添加Let&#x27;s</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">think</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">step-by-step</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Language</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Models</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">are</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Zero-Shot</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Reasoners.”Advances</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">in</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Neural</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Information</span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Processing</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Systems</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">35</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">(NeurIPS</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">2022):</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">22199-22213.</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">（让我们一步一步地思考）</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">这样的提示词，可以进一步简化该推理过程。类似地，这种方式改变</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">了语言模型的提议分布，使其倾向于在正式回答问题前先将推理过程</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">分解为多个步骤，如图1-3
6所示。</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">然而，仅仅通过添加提示词的方法，模型本身并未真正学会遵循这一</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">流程。此外，这种静态且线性的流程会阻碍模型的自我修正——如果模</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图1-35</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">通过提示工程调整提议分布</span>

<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Kojima,</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Takeshi,</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">et</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">al.“Large</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图1-36</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">添加隐式促进推理的提示词</span>

![](./图解DeepSeek技术_assets/images/p048_img01.jpg)



![](./图解DeepSeek技术_assets/images/p048_img02.png)



![](./图解DeepSeek技术_assets/images/p048_img03.jpg)

---

## Page 49

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">型在初始阶段就采用了错误的推理路径，它往往会延续错误，而不是</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">对其进行修正。</span>
<span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000">1.5.2</span><span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000">STaR方法</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">除了使用提示工程，我们还可以通过训练模型生成推理步骤并给予奖</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">励的方式，引导模型学会“推理”。这种方法通常需要大量的推理数</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">据，并借助强化学习来奖励特定的行为。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">一种备受争议的技术是STaR（Self-Taught</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">Reasoner</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">et</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">al.“STaR:</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Bootstrapping</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Reasoning</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">With</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Reasoning.”Advances</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">in</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Neural</span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Information</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Processing</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Systems</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">35</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">(2022):</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">15476-15488.</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">，自我教导推理器）方</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">法。该方法利用大模型自动生成推理数据，并以此作为微调模型的输</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">入，其流程大体如下所示。</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">生成推理步骤和答案。若答案正确</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">，则将推理过程和答案组合成三</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">元组训练数据集</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">，这些数据随后用于对模型进行监督微调</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">（supervised</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">fine</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">tuning，SFT）</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span>

<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Zelikman,</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Eric,</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">，如图1-3
7所示。</span>

![](./图解DeepSeek技术_assets/images/p049_img01.png)



![](./图解DeepSeek技术_assets/images/p049_img02.jpg)



![](./图解DeepSeek技术_assets/images/p049_img03.jpg)



![](./图解DeepSeek技术_assets/images/p049_img04.jpg)



![](./图解DeepSeek技术_assets/images/p049_img05.jpg)

---

## Page 50

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图1-37</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">STaR（1）</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">然而，若模型给出错误答案</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">，我们则提供“提示”（即正确答案）</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">并要求模型推理该答案为何正确</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">。同样地，这里构造的三元组训练</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">数据将用于对模型进行监督微调</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">。推理模型的训练流程如图1-38所</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">示。</span>

![](./图解DeepSeek技术_assets/images/p050_img01.jpg)



![](./图解DeepSeek技术_assets/images/p050_img02.jpg)



![](./图解DeepSeek技术_assets/images/p050_img03.jpg)



![](./图解DeepSeek技术_assets/images/p050_img04.jpg)

---

## Page 51

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">该技术的核心要素（配合多种改进提议分布的方法）在于：我们通过</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">显式训练使模型能够遵循所展示的推理过程。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">换言之，我们通过监督微调的方式确定了推理过程的具体形态。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">整个技术流程尤为有趣，其本质是在生成合成训练样本。使用合成训</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">练样本（正如我们将在接下来DeepSeek-R1的相关内容中探讨的）是</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">一种卓越的方法，可将这种推理过程蒸馏</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">称为“教师模型”）中学到的知识传递给较小模型（通常称为“学生模型”）的技术，其目标</span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">是在保留性能的同时显著减少模型的体积和推理开销。——译者注</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">（distill）到其他模</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">型中。</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图1-38</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">STaR（2）</span>

<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">模型蒸馏是一种将较大模型（通常</span>

![](./图解DeepSeek技术_assets/images/p051_img01.jpg)



![](./图解DeepSeek技术_assets/images/p051_img02.png)

---

## Page 52

<span style="font-family:'Unnamed-T3';font-size:16.88pt;color:#000000">1.6</span><span style="font-family:'Unnamed-T3';font-size:16.88pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:16.88pt;color:#000000">小结</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">本章首先介绍了推理大模型的概念、发展及其与传统大模型的区别。</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">推理大模型通过将问题分解为更小的推理步骤或思考过程，从而提升</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">模型在复杂任务中的表现。这种模型的核心在于学习“如何回答”，</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">而非仅仅“回答什么”。</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">本章还详细介绍了从训练时计算到测试时计算的范式转变。传统的训</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">练时计算主要依赖增加模型参数量、数据量和计算量来提升性能，但</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">随着规模扩大，边际收益递减，研究人员开始关注测试时计算。测试</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">时计算的目标是优化模型在推理阶段的表现，生成更多的中间思考步</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">骤或上下文信息，从而提升最终输出的准确性。</span>

---

## Page 53

<span style="font-family:'Unnamed-T3';font-size:16.88pt;color:#000000">1.7</span><span style="font-family:'Unnamed-T3';font-size:16.88pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:16.88pt;color:#000000">延伸阅读</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">希望本章能为你理解推理大模型提供一个通俗易懂的入门指南。若需</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">深入研究，建议你参考以下资源。</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000"> </span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">Machines</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">Lab联合创始人、OpenAI前安全副总裁Lilian</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">Weng的博客文章“Why</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">We</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">Think”（发表于2025年5月）深入讲解了测试时计算和推理模型的最新研究进展。</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">•</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">如果</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">你需要训练推理语言模型，verl和OpenRLHF是两个不错的开源框架。</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">•</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">如果你希望用少</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">量计算资源就可以自己动手训练一个推理语言模型，TinyZero是一个不错的开始。它仅依赖</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">一个不具备推理能力的开源3B基座模型，而你只需花费几十美元的GPU成本，就能让该模型</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">学会求解
24点问题和大整数乘法——这类任务通常难以仅通过提示词完成。——译者注</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">Hugging</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">Face团队发布的技术文章“Scaling</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">Test-</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">Time</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">Compute</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">with</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">Open</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">Models”（《使用开源模型扩展测试时</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">计算》），通过实验探讨测试时计算的扩展策略。</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">YouTube视频“Speculations</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">on</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">Test-Time</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">Scaling</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">(o1)”（“测</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">试时扩展的技术推演（OpenAI</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">o1）”）深入解析常见测试时计算技</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">术的实现细节。</span>

<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">更多可参考资源如下。</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">•</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">Thinking</span>

![](./图解DeepSeek技术_assets/images/p053_img01.png)

---

## Page 54

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">第2章</span>
<span style="font-family:'Unnamed-T3';font-size:21.01pt;color:#000000">架构设计</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">DeepSeek-R1与GPT-2和GPT-3等早期模型一脉相承，同样采用了</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">Transformer解码器模块的堆叠架构。DeepSeek-R1模型包含61层</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">Transformer解码器模块，其中前
3层为稠密层，其余
58层均为MoE</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">（Mixture-of-Experts，混合专家）层，如图2-1所示。</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图2-1</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">DeepSeek-R1整体架构</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">在本章中，我们将从前
3层着手深入探讨DeepSeek-R1的架构设计。</span>

![](./图解DeepSeek技术_assets/images/p054_img01.jpg)

---

## Page 55

<span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000">2.1</span><span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000">稠密层</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">前3层Transformer块的结构与当前主流大模型相似，且规模一致。每</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">个Transformer块首先对输入采用均方根层归一化</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">在神经网络中对输入进行归一化的技术。它的实现方式是先计算输入张量中每个元素的平方的</span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">均值，然后取其平方根（RMS），最后用输入除以这个RMS值并乘以一个可学习的缩放因</span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">子。均方根层归一化的目的是稳定训练过程，加速模型收敛。——译者注</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">（root</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">mean</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">square</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">layer</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">normalization，RMSNorm）操作（简</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">称RMS归一化），以实现稳定且高效的归一化过程。完成注意力机制</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">运算后，系统会再次执行归一化操作。经过这些关键步骤形成的特征</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">表示，最终会送入前馈神经网络（feed</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">forward</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">neural</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">network，</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">FFNN）进行深度处理。DeepSeek-R1稠密层结构如图2-2所示。</span>

<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">均方根层归一化是一种</span>

![](./图解DeepSeek技术_assets/images/p055_img01.png)



![](./图解DeepSeek技术_assets/images/p055_img02.jpg)

---

## Page 56

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图2-2</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">DeepSeek-R1稠密层结构</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">虽然大部分子模块的结构与当前主流大模型的子模块的结构类似，但</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">存在一个关键区别——DeepSeek-R1的注意力模块采用的是MLA</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">（multi-head</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">latent</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">attention，多头潜在注意力）机制，而非传统</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">的多头注意力机制。MLA最早应用于DeepSeek-V2模型中，也就是</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">DeepSeek-R1的基础模型DeepSeek-V3的前身</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">“DeepSeek-V2:</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">A</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">strong,</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">economical,</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">and</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">efficient</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">mixture-of-experts</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">language</span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">model.”arXiv</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">preprint</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">arXiv:2405.04434</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">(2024).</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">。MLA通过对K（key，键）和</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">V（value，值）进行低秩联合压缩，有效降低了推理过程中的KV</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">（键</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">-</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">值）缓存需求。MLA可视为对多头注意力机制核心部分的压缩</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">处理方法。</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">在DeepSeek-V3中，MLA把7168维的K、V分别压缩为512维，把7168维的Q压缩为1536维。</span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">——译者注</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">稠密层注意力子模块的结构如图2-3所示。</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">在此压缩过程中，模型会对KV进行缓存。这种KV缓存机制使得推理过</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">程更加高效——当生成新词元时，可以直接利用缓存值来快速更新注意</span>

<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Liu,</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Aixin,</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">et</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">al.</span>

<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">具体来说，MLA对注意力机制中的Q、K、V向量分别做了低秩投影。例如</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图2-3</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">稠密层注意力子模块的结构</span>

![](./图解DeepSeek技术_assets/images/p056_img01.png)



![](./图解DeepSeek技术_assets/images/p056_img02.png)



![](./图解DeepSeek技术_assets/images/p056_img03.jpg)

---

## Page 57

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">力矩阵。</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">求。由于Q和K都经过RoPE处理而变得对位置高度敏感，如果直接对压缩后的K应用RoPE，</span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">会导致其同样具备位置敏感性，从而在推理时必须重新计算所有前缀词元的K。这意味着无法</span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">使用KV缓存，严重影响推理效率。为了解决这个问题，MLA引入了一个维度较小的额外位置</span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">向量（如图中红框所示），仅对其应用RoPE编码，并采用类似MQA（多头共享参数）的方式</span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">将其与不含位置信息的低秩K向量拼接。这种结构既能补充位置信息，又避免了对整个K向量</span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">重新编码，从而保留KV缓存机制，提升推理效率。在DeepSeek-V3中，512维的低秩KV向量</span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">中额外添加了
64维的位置编码向量。——译者注</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">该方法不仅避免了重新计算已生成</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">词元的开销，其效率也超越了先前诸如GQA</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">（grouped</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">query</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">attention，分组查询注意力）等方法。</span>

<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">使用低秩KV并不简单，主要挑战在于RoPE（旋转位置嵌入）对位置信息的要</span>

![](./图解DeepSeek技术_assets/images/p057_img01.png)

---

## Page 58

<span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000">2.2</span><span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000">MoE层</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">DeepSeek-R1的其余
58层与前
3层结构相似，但存在一个关键差异：</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">这些层没有采用常规的前馈神经网络，而是使用了MoE机制，如图2-4</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">所示。</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图2-4</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">DeepSeek-R1的MoE层结构</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">MoE是一种通过使用多个不同的子模型（也称为“专家”）来提升大</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">模型质量的技术，它由两大核心组件构成。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">专家</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">每个前馈神经网络层由一组“专家”组成，具体计算时可从中选</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">择一个子集。这些专家本身并非完整的大模型，而是大模型架构中</span>

![](./图解DeepSeek技术_assets/images/p058_img01.jpg)

---

## Page 59

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">MoE的组成部分，通常本身也是前馈神经网络结构。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">路由器</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">（也称为门控网络）负责决定将词元分配给特定的专家模块。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">如图2-5所示，在采用MoE机制的每个MoE层中，都有若干个专家。</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图2-5</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">每个MoE层中存在若干个专家</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">需要注意的是，所谓“专家”的“专”并非指心理学或生物学等特定</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">领域的专业人士，充其量，它只是在单词层面上学习一些句法信息而</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">已</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Zoph,</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Barret,</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">et</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">al.“ST-MoE:</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Designing</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Stable</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">and</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Transferable</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Sparse</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Expert</span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Models.</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">arXiv</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">2022.”arXiv</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">preprint</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">arXiv:2202.08906.</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">，如图2-6所示。</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图2-6</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">专精于单词层面的句法信息的不同专家</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">更准确地说，专家是能够在特定上下文中处理特定词元的子模块。路</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">由器（门控网络）会根据输入的内容，选择最合适的专家，如图2-7所</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">示。</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Transformer的每一层都有自己独立的路由器和一组专家。根据该层路由器的选择，</span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">每一层每处理一个词元，都会有一个或多个专家被激活。不同词元、不同层激活的专家未必相</span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">同。——译者注</span>

![](./图解DeepSeek技术_assets/images/p059_img01.jpg)



![](./图解DeepSeek技术_assets/images/p059_img02.png)



![](./图解DeepSeek技术_assets/images/p059_img03.jpg)



![](./图解DeepSeek技术_assets/images/p059_img04.png)

---

## Page 60

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图2-7</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">路由器根据输入选择最合适的专家</span>

<span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000">2.2.1</span><span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000">专家机制</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">为理解专家的本质及其运作原理，我们首先回顾一下MoE所替代的传</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">统稠密层。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">MoE起源于大模型中一个相对基础的组成部分：前馈神经网络。在标</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">准的仅包含解码器的Transformer架构中，前馈神经网络（FFNN）</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">通常位于层归一化操作之后，如图2-8所示。</span>

![](./图解DeepSeek技术_assets/images/p060_img01.jpg)

---

## Page 61

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图2-8</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">仅包含解码器的Transformer架构</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">前馈神经网络使模型能够利用注意力机制所生成的上下文信息，并对</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">其进行进一步变换，以捕捉数据中更为复杂的关系。但为了学习这些</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">更为复杂的关系，前馈神经网络通常需要扩展其接收的输入，因此模</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">型规模会迅速增长，如图2-9所示。</span>

![](./图解DeepSeek技术_assets/images/p061_img01.jpg)

---

## Page 62

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图2-9</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">传统Transformer中的前馈神经网络的稠密模型架构</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">传统Transformer中的前馈神经网络之所以被称为稠密模型，是因为</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">其所有参数（包括权重和偏置）都会被激活，如图2-1
0所示。换句话</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">说，所有参数都会参与输出的计算，激活程度虽然不同，但没有任何</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">部分被闲置。</span>

![](./图解DeepSeek技术_assets/images/p062_img01.jpg)

---

## Page 63

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图2-10</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">稠密模型：所有参数都会被激活</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">相比之下，稀疏模型仅激活部分参数，其机制与MoE密切相关。为了</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">说明这一点，我们可以将稠密模型切割成若干部分（即所谓的专</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">家），对其进行重新训练，并在给定时刻仅激活其中一部分专家，如</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图2-1
1所示。</span>

![](./图解DeepSeek技术_assets/images/p063_img01.jpg)

---

## Page 64

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图2-11</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">稀疏模型</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">这一机制背后的核心思想是：每个专家在训练过程中会学习到不同的</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">知识。在推理过程中，模型只激活与当前任务相关性最强的专家，从</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">而实现更高效的计算。举例来说，当模型接收到一个问题时，它会挑</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">选最擅长处理该类任务的专家来生成回答，如图2-1
2所示。</span>

![](./图解DeepSeek技术_assets/images/p064_img01.jpg)

---

## Page 65

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图2-12</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">在推理时，模型挑选最合适的专家来生成回答</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">为了方便理解，我们可以把专家想象成：把一个稠密模型中的隐藏层</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">（比如一个大矩阵）切成很多小块，每个小块负责一部分工作。但实</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">际上，在真实的稀疏模型设计里，每个专家往往是一个完整的前馈神</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">经网络，而不是单纯的“某一层的碎片”，如图2-1
3所示。</span>

![](./图解DeepSeek技术_assets/images/p065_img01.jpg)

---

## Page 66

What  is[1|+1丨?
稀疏模型
4个专家
（每个专家都是一个完整的
前馈神经网络）
标点符号

未激活  未激活  未激活  已激活

<!-- 图源: ./图解DeepSeek技术_assets/images/p066_scan.jpg -->


<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图2-13</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">专家是完整的前馈神经网络</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">大部分大模型通常包含多个解码器，在生成最终输出之前，输入文本</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">会依次经过多个专家处理，如图2-1
4所示。</span>

---

## Page 67

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图2-14</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">在生成最终输出之前，输入文本通常会经过多个专家处理</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">所选专家因词元而异，从而导致激活的执行“路径”有所不同，如图2-</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">15所示。</span>

![](./图解DeepSeek技术_assets/images/p067_img01.jpg)

---

## Page 68

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图2-15</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">所选专家因词元而异，从而激活不同的执行路径</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">简单总结一下，采用MoE的解码器如图2-1
6所示，其用多个（图中为4</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">个）前馈神经网络替换了稠密模型中单一的前馈神经网络结构，其中</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">每个前馈神经网络表示一个专家，可在推理过程中被调用。</span>

![](./图解DeepSeek技术_assets/images/p068_img01.jpg)

---

## Page 69

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图2-16</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">采用MoE的解码器</span>

<span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000">2.2.2</span><span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000">路由机制</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">在拥有了一组专家后，模型需要决定该使用哪些专家。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">为此，MoE在专家网络层之前引入了一个称为路由器（也叫门控网</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">络）的组件。路由器本身也是一个前馈神经网络，它经过训练后根据</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">输入内容输出概率分布，从而挑选最匹配的专家进行处理。专家层的</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">输出是由被选中的专家所产生的结果与其对应的门控值（概率）相乘</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">而得到的，如图2-1
7所示。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">路由器与多个（图中为4个）专家共同组成了MoE层，其中仅少数（图</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">中为1个）专家会被选，如图2-1
8所示。</span>

![](./图解DeepSeek技术_assets/images/p069_img01.jpg)

---

## Page 70

MoE层
路由器
FFNN
softmax

0.45
0.31  每个专家的
激活  0.19  概率分布
选中的
专家
未激活
FFNN1FNN  FFNN3FFNN

p=0.45  加权激活

<!-- 图源: ./图解DeepSeek技术_assets/images/p070_scan.jpg -->


<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">▲图2-17</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">路由器处理流程</span>

---

## Page 71

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">▲图2-18</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">MoE层</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">MoE层通常有两种配置方式：稀疏混合和稠密混合。两者均采用路由</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">器来选择专家，不同的是，稀疏MoE激活少量专家进行处理，而稠密</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">MoE则会激活所有专家，只是专家的参与程度根据输入而有所不同。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">也就是说，给定一组词元，稠密MoE会将这些词元分配给所有专家，</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">而稀疏MoE只会选择少数专家对词元进行处理，如图2-1
9所示。</span>

![](./图解DeepSeek技术_assets/images/p071_img01.jpg)

---

## Page 72

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图2-19</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">稀疏MoE与稠密MoE</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">当前大模型中所提到的MoE通常指的是稀疏MoE，因为这种架构允许</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">使用部分专家进行处理，从而降低计算成本，这对大模型而言非常重</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">要。MoE中的路由器无疑是最为关键的部分，它不仅在推理阶段决定</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">了选择哪些专家，在训练阶段也发挥着同样的作用。我们来看看路由</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">器最基础的实现形式，先计算输出矩阵——将输入矩阵</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">与路由器权重</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">矩阵</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">相乘，如图2-2
0所示。</span>

![](./图解DeepSeek技术_assets/images/p072_img01.jpg)



![](./图解DeepSeek技术_assets/images/p072_img02.jpg)



![](./图解DeepSeek技术_assets/images/p072_img03.jpg)

---

## Page 73

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图2-20</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">计算输出矩阵</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">接着对输出结果施加softmax函数，从而为每个专家生成一个概率分</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">布，如图2-2
1所示。</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图2-21</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">每个专家的概率分布</span>

![](./图解DeepSeek技术_assets/images/p073_img01.jpg)



![](./图解DeepSeek技术_assets/images/p073_img02.jpg)



![](./图解DeepSeek技术_assets/images/p073_img03.jpg)

---

## Page 74

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">路由器利用这一概率分布为给定输入选择最匹配的专家。最终，路由</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">器的输出（即选择概率）与各被选中的专家的输出相乘，并对结果进</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">行加权求和，从而生成最终的输出，如图2-2
2所示。</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图2-22</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">最终的输出</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">综合前述机制，给定输入矩阵</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">生成的完整路径如图2-23</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">所示。</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">，输出结果</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span>

![](./图解DeepSeek技术_assets/images/p074_img01.jpg)



![](./图解DeepSeek技术_assets/images/p074_img02.jpg)



![](./图解DeepSeek技术_assets/images/p074_img03.jpg)

---

## Page 75

MoE层

路由器

H(x)
softmax

x)

FFNN1FFNN2FFNN3FFNN4
E(x)

G(x)+β

<!-- 图源: ./图解DeepSeek技术_assets/images/p075_scan.jpg -->

---

## Page 76

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图2-23</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">给定输入矩阵</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">，输出结果</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span>

<span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000">2.2.3</span><span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000">DeepSeekMoE</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">在了解了MoE机制后，我们接下来将探索DeepSeek所采用的MoE架</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">构。该架构最早在其拥有164亿个参数的DeepSeekMoE</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">al.“DeepSeekMoE:</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Towards</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Ultimate</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Expert</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Specialization</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">in</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Mixture-of-Experts</span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Language</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Models.”arXiv</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">preprint</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">arXiv:2401.06066</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">(2024).</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">模型中提出。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">DeepSeek-R1中MoE的基础是Top-K路由策略。在该机制中，路由器</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">会简单地选择得分最高的K个专家，并通过聚合这些专家的输出结果完</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">成计算，如图2-2
4所示。</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">生成的完整路径</span>

<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Dai,</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Damai,</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">et</span>

![](./图解DeepSeek技术_assets/images/p076_img01.jpg)



![](./图解DeepSeek技术_assets/images/p076_img02.jpg)



![](./图解DeepSeek技术_assets/images/p076_img03.png)

---

## Page 77

路由器

Top-K路由

<!-- 图源: ./图解DeepSeek技术_assets/images/p077_scan.jpg -->

---

## Page 78

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图2-24</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">Top-K路由机制</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">DeepSeekMoE研究团队指出：当专家数量较少时，每个专家往往会</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">覆盖多种类型的知识。这就使得单个专家难以有效运用其广泛的知识</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">储备。研究团队认为增加专家数量有利于每个专家学到更加专业的知</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">识。为此，研究团队将专家分解为更小的专家单元，即通过降低隐藏</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">层维度（采用更精简的前馈神经网络结构）创建更多更小型的专家，</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">从而促使每个专家能够专注于学习特定领域的专业知识。这一方法被</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">称为细粒度专家分割（fine-grained</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">expert</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">segmentation），如图</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">2-2
5所示。</span>

---

## Page 79

路由器

细粒度
专家分割

<!-- 图源: ./图解DeepSeek技术_assets/images/p079_scan.jpg -->

---

## Page 80

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">需要注意的是，这种方法在计算成本上与之前的方法一样。相较于使</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">用少数大型专家，系统采用了多个小型专家，但专家的总参数量保持</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">不变。因此，为了维持相同的计算成本，需要激活更多专家参与计</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">算。然而，该策略也带来一个问题，每个专家仍需学习一定量的通用</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">知识，这可能导致不同专家之间在知识上存在冗余，从而在一定程度</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">上削弱了专家学习专业知识的能力。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">为解决这一问题，DeepSeek提出了一种简洁且有效的方案：将部分专</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">家显式设定为共享专家（shared</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">experts</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">“DeepSpeed-MoE:</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Advancing</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Mixture-of-Experts</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Inference</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">and</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Training</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">to</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Power</span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Next-Generation</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">AI</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Scale.”</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">International</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Conference</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">on</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Machine</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Learning.</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">PMLR,</span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">2022.</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">）。这些共享专家在处理所有词元的过程中始终处于激活状态，因</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">此具备天然的动机去学习通用性更强的知识。这种方法被称为共享专</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">家隔离（shared</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">expert</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">isolation），它有效地缓解了知识冗余问</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">题，并提升了其他专家专注于特定任务的能力，如图2-2
6所示。</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图2-25</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">细粒度专家分割</span>

<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Rajbhandari,</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Samyam,</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">et</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">al.</span>

![](./图解DeepSeek技术_assets/images/p080_img01.png)

---

## Page 81

路由器

共享
专家隔离

<!-- 图源: ./图解DeepSeek技术_assets/images/p081_scan.jpg -->

---

## Page 82

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">通过结合使用上述三种方法，DeepSeek实现了专家功能的有效分工：</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">由路由器选择的专家通过细粒度专家分割专注于学习更加专业化的知</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">识，而共享专家则通过共享专家隔离机制被激励去学习通用性知识，</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">如图2-2
7所示。</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图2-27</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">DeepSeek结合三种方法实现专家的有效分工</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">然而，即使引入了上述改进，仍无法保证路由器每次都会选择一组多</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">样化的专家。在某些情况下，可能无论输入什么样的内容，路由器都</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">反复选择同一组专家，导致模型过度依赖少数几个专家，这一现象被</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">称为路由崩塌（routing</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">collapse</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Large</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Neural</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Networks:</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">The</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Sparsely-Gated</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Mixture-of-Experts</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Layer.”arXiv</span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">preprint</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">arXiv:1701.06538</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">(2017).</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">），如图2-2
8所示。</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图2-26</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">共享专家隔离机制</span>

<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Shazeer,</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Noam,</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">et</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">al.“Outrageously</span>

![](./图解DeepSeek技术_assets/images/p082_img01.jpg)



![](./图解DeepSeek技术_assets/images/p082_img02.png)

---

## Page 83

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">为了在训练与推理阶段实现专家之间的均衡利用，DeepSeekMoE引</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">入了负载均衡机制。在训练阶段，为了使专家分布更加均匀，</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">DeepSeekMoE在常规损失函数的基础上引入了两种辅助损失函数</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">（也称为负载均衡损失）：其一为专家级损失（expert-level</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">loss），</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">用于鼓励模型持续使用多样化的专家组合，防止路由崩塌现象的发生</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">（然而，当使用大量GPU并行训练时，由于施加了过多约束，这种强</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">制性的均衡可能会导致性能下降）；其二为设备级损失（device-</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">level</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">loss），用以促进不同设备</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">注</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">间的负载均衡，从而提高整体效率。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">至此，DeepSeek-R1的主要组成部分就介绍完毕！让我们简要回顾一</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">下目前所了解的内容：模型前
3层为Transformer稠密层，但引入了多</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">头潜在注意力机制；其余
58层为MoE层，同样采用了多头潜在注意力</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">机制，如图2-2
9所示。</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图2-28</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">路由崩塌</span>

<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">这里的设备指的是用于训练的GPU。——译者</span>

![](./图解DeepSeek技术_assets/images/p083_img01.jpg)



![](./图解DeepSeek技术_assets/images/p083_img02.png)

---

## Page 84

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图2-29</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">DeepSeek-R1的主要组成部分</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">模型维度大小及其他超参数的配置情况如图2-3
0所示，其中包含一个</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">共享专家和256个小型专家，每次处理任务将会激活其中的
8个专家。</span>

![](./图解DeepSeek技术_assets/images/p084_img01.jpg)

---

## Page 85

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图2-30</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">DeepSeek-R1模型维度及超参数配置</span>

![](./图解DeepSeek技术_assets/images/p085_img01.jpg)

---

## Page 86

<span style="font-family:'Unnamed-T3';font-size:16.88pt;color:#000000">2.3</span><span style="font-family:'Unnamed-T3';font-size:16.88pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:16.88pt;color:#000000">小结</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">DeepSeek-R1模型基于Transformer解码器模块堆叠架构，共61层，</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">前
3层为稠密层，后
58层为MoE层。</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">稠密层：前3层Transformer块结构与主流大模型类似。每个块先对</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">输入进行RMS归一化操作，再经多头潜在注意力机制运算，该机制通</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">过低秩联合压缩键和值来降低KV缓存需求，提高推理效率，最后送入</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">前馈神经网络处理。</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">MoE层：后
58层采用MoE机制替代常规的前馈神经网络。MoE由专</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">家和路由器组成，专家实际上也是前馈神经网络，而路由器负责将词</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">元分配给特定专家。</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">下一章，我们来具体看看DeepSeek-R1的训练方案。</span>

---

## Page 87

<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">第3章</span>

<span style="font-family:'Unnamed-T3';font-size:21.25pt;color:#000000">DeepSeek-R1训练方案</span>

<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">DeepSeek-R1无疑是推理模型领域的重大突破——这个开源模型的权</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">重已全面公开</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000"> </span>

<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">Guo,</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">Daya,</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">et</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">al.“DeepSeek-R1:</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">Incentivizing</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">Reasoning</span>

<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">Capability</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">in</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">LLMs</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">via</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">Reinforcement</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">Learning.”arXiv</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">preprint</span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">arXiv:2501.12948</span>
<span style="font-family:'Unnamed-T3';font-size:11.25pt;color:#000000">(2025).</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">。作为OpenAI</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">o1推理模型的直接竞品，DeepSeek-R1对行业</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">产生了深远影响。</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">DeepSeek研究团队通过多项技术，成功将推理能力优雅地蒸馏至其</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">基座模型（DeepSeek-V3-Base），展现出非凡的技术造诣。</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">值得注意的是，该过程未使用验证器，且未采用监督微调来蒸馏推理</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">行为，而是重点聚焦于强化学习。接下来让我们深入探究DeepSeek</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">研究团队是如何在模型中训练出推理能力的。</span>

![](./图解DeepSeek技术_assets/images/p087_img01.png)

---

## Page 88

<span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000">3.1</span><span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000">回顾：大模型的训练原理</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">与现有大多数大模型相同，DeepSeek-R1的训练采用了逐词元生成的</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">范式。但DeepSeek-R1在处理数学与推理问题上表现尤为出色，这种</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">能力源于特殊的训练方法——它能够生成可以解释其思维链的“思考词</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">元”（thinking</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">token），这样就能花更多时间来深入处理问题，如</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图3-1所示。</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图3-1</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">思考词元</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图3-2展示了构建高质量大模型的通用方案，具体流程主要包含三个关</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">键阶段。</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图3-2</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">构建高质量LLM的三个关键阶段</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">语言建模阶段：通过海量网络数据训练模型预测下一个词元，该阶段</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">产出基座模型。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">监督微调阶段：使模型在遵循指令和回答问题方面更加实用，该阶段</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">产出指令调优模型（instruction</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">tuned</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">model）或监督微调模型</span>

![](./图解DeepSeek技术_assets/images/p088_img01.jpg)



![](./图解DeepSeek技术_assets/images/p088_img02.jpg)

---

## Page 89

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">（SFT</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">model）。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">偏好调优阶段：最终通过人类偏好对齐进一步优化模型行为，生成可</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">供应用程序交互的偏好调优大模型（preference-tuned</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">LLM）。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">DeepSeek-R1遵循这一通用方案，其中第一阶段的具体实施细节源自</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">DeepSeek-V3模型的相关论文“DeepSeek-</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">V3</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">Technical</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">Report”。DeepSeek-R1使用该论文中的基座模型</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">（而非最终的DeepSeek-V3模型），仍然经过监督微调和偏好调优阶</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">段，但其具体实施方法存在创新性差异，如图3-3所示。</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图3-3</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">DeepSeek-R1的训练过程</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">请注意，在DeepSeek-R1的训练过程中有三个特别之处，接下来我们</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">分别聊一聊。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">1.</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">长推理链监督微调数据</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">基于基座模型进行监督微调需要用到数量庞大的长链思维推理示例</span><span style="font-family:'Unnamed-T3';font-size:8.65pt;color:#000000">[1]</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">（DeepSeek-R1的训练过程一共用到了
60万个），如图3-4所示。如此</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">规模的标注工作意味着，一方面数据极难获取，另一方面人工标注成</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">本极为高昂。这也正是生成这些数据的过程成为值得强调的创新点的</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">原因。</span>

![](./图解DeepSeek技术_assets/images/p089_img01.jpg)

---

## Page 90

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图3-4</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">推理数据：长链思维示例</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">2.</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">临时性高质量推理大模型</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">上述长链思维推理数据由DeepSeek-R1模型的前身生成，这是一个未</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">命名的临时模型，专精于推理任务，其设计灵感源于另一个名为</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">DeepSeek-R1-Zero的模型（稍后我们将详细讨论）。这个临时模型的</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">重大意义不在于它作为一个实用的大模型具备多好的性能，而在于其</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">构建过程仅需少量标注数据，配合大规模强化学习训练，最终造就了</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">一个擅长解决推理问题的模型。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">通过使用这个未命名的专精于推理（在非推理任务上表现欠佳）的临</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">时模型生成输出结果，开发人员得以训练出更通用的模型，如图3-5所</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">示。最终得到的通用模型不仅能完成推理任务，同时，在非推理任务</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">的处理水平上，也能达到用户对大模型的预期。</span>

![](./图解DeepSeek技术_assets/images/p090_img01.jpg)

---

## Page 91

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图3-5</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">专精于推理的临时推理模型</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">3.</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">利用大规模推理导向的强化学习构建推理模型</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">构建临时推理模型的关键就在于大规模推理导向的强化学习，如图3-6</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">所示。</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图3-6</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">推理导向的强化学习</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">这第三点非常重要，因为它促成了一个有趣的、专注于推理能力的模</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">型——DeepSeek-R1-Zero。在介绍DeepSeek-R1的完整训练流程之</span>

![](./图解DeepSeek技术_assets/images/p091_img01.jpg)



![](./图解DeepSeek技术_assets/images/p091_img02.jpg)

---

## Page 92

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">前，我们先来看看DeepSeek-R1-Zero的具体细节。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">[1]对于这里的“示例”（example）一词，原始论文用的是“样本”</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">（sample），本书其他部分使用的也是“样本”。作者在这一部分使</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">用“示例”一词旨在说明强化学习训练的目的是让模型模仿这些示例</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">的推理过程。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">以下为一个思维链推理示例：</span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">用户输入：</span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">I</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">have</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">10</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">apples.</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">I</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">gave</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">2</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">apples</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">away.</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">I</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">then</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">went</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">and</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">bought</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">5</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">more</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">apples</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">and</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">ate</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000"> </span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">1.</span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">How</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">many</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">apples</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">do</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">I</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">have?</span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">思维链推理：</span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">&lt;think&gt;</span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">First,</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">you</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">started</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">with</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">10</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">apples.</span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">You</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">gave</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">away</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">2</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">apples</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">→</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">10</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">-</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">2</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">=</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">8.</span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Then,</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">you</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">bought</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">5</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">more</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">→</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">8</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">+</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">5</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">=</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">13.</span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Then,</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">you</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">ate</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">1</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">→</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">13</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">-</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">1</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">=</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">12.</span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">&lt;/think&gt;</span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">最终回答：</span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">&lt;answer&gt;12&lt;/answer&gt;</span>

---

## Page 93

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">——译者注</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">3</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">推理导向（reasoning-oriented）的强化学习是指要求模型先输出推</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">理过程，后输出答案的强化学习过程。这与RLHF（基于人类反馈的强</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">化学习）有很大的不同，RLHF的目的是让模型的输出与人类的偏好和</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">价值观对齐，从而让模型的输出格式和表达方式对人类更友好，并拒</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">绝输出有害的内容；而推理导向的强化学习的目的是让模型解决数</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">学、编程、逻辑推理等需要思考才能解决的问题。——译者注</span>

---

## Page 94

<span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000">3.2</span><span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000">DeepSeek-R1-Zero的推理能力</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">通向DeepSeek-R1的重要突破源自一个实验性模型——DeepSeek-R1-</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">Zero。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">DeepSeek-R1-Zero的特殊性在于，它无须依赖标注的监督微调训练</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">集，就能在推理任务中表现得很出色。它的训练过程直接从预训练的</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">基座模型开始，通过强化学习训练流程完成（跳过了监督微调阶</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">段）。它的表现非常出色，甚至能够与OpenAI</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">o1媲美，如图3-7所</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">示。</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">该截图中的表来自论文“DeepSeek-</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">R1:</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">Incentivizing</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">Reasoning</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">Capability</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">in</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">LLMs</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">via</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">Reinforcem</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">ent</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">Learning”，展示的是DeepSeek-R1-Zero和OpenAI</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">o1在推理</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">相关基准测试上的表现对比。我们在后续内容中还会引用这篇论文中</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">的内容，不妨将其简称为“DeepSeek-R1论文”。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图3-7</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">DeepSeek-R1-Zero和OpenAI</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">o1在推理相关基准测试上的表现</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">对比</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">DeepSeek-R1-Zero以DeepSeek-V3-Base为基础，摒弃了对大量推理</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">数据进行监督微调的传统方法，仅通过强化学习即可实现推理能力。</span>

![](./图解DeepSeek技术_assets/images/p094_img01.jpg)

---

## Page 95

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">为实现这一目标，研究团队在训练流程中采用了一个极其简洁的提示</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">词（类似于系统提示词），如图3-8所示。</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图3-8</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">DeepSeek-R1-Zero训练流程中的类系统提示词</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">请注意，研究团队明确提到推理过程应位于&lt;think&gt;&lt;/think&gt;标签之间</span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">DeepSeek-R1-Zero训练过程中并没有规定推理过程的具体形式，也没有通过监督微调提供</span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">推理过程的示例。具体的推理过程完全是模型在强化学习过程中自己思考出来的，并通过奖励</span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">机制强化了正确的推理过程。——译者注</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">在强化学习阶段，研究团队设计了两种基于规则的奖励机制：</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">准确性奖励（accuracy</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">rewards）——通过实际测试对答案进行奖</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">励；</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">格式奖励（format</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">rewards）——对正确使用&lt;think&gt;和&lt;answer&gt;标</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">签的行为进行奖励（在本书的图中标注为</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">&lt;</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">格式</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">&gt;</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">奖励）。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">这两种奖励机制会在后续内容反复体现，此处先不展开说明。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">DeepSeek-R1-Zero为何能够不依赖监督微调？这指向两个关键事实。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">现代基座模型已经跨越了某个门槛，在质量和能力上有了质的提升</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">（DeepSeek-V3-Base基于14.8万亿个高质量词元进行训练）。</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">模型基础上进行强化学习训练是强化学习领域的重要里程碑。在传统强化学习中，模型一般是</span>

<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">在大</span>

![](./图解DeepSeek技术_assets/images/p095_img01.jpg)



![](./图解DeepSeek技术_assets/images/p095_img02.png)



![](./图解DeepSeek技术_assets/images/p095_img03.png)

---

## Page 96

<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">从头训练的，不具备常识和自然语言理解能力，因此在处理涉及现实世界的问题时，强化学习</span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">训练的效率往往很低。例如，OpenAI研究科学家姚顺雨在“The</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Second</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Half”一文中举过</span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">一个例子：在游戏中，人类可以理解箱子、武器和恶龙之间的关系，因此看到恶龙就会寻找藏</span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">有武器的箱子；而不具备常识和自然语言理解能力的模型仅仅把游戏中的道具当作一个没有意</span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">义的符号，从而需要很多次尝试才能学到游戏的玩法，学习效率很低。——译者注</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">与通用对话或写作需求不同，推理问题可以实现自动验证与标注。</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">化学习的关键在于具备奖励函数，即模型能够根据结果获得反馈，从而判断行为的好坏，进而</span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">从正确和错误的样本中学习。数学、编程、逻辑推理等问题容易构造结果奖励函数，而通用对</span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">话和写作需求难以自动评判对错。——译者注</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">让我们通过具体示例加以说明。</span>
<span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000">3.2.1</span><span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000">示例：推理问题的自动验证</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">在强化学习训练阶段，假设给出如下提示词（或者输入如下问题）：</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">此类问题天然适合多种自动验证方式。假设我们将该问题输入正在训</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">练的模型，并得到一个输出，以下多种方法都可以对输出做自动验</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">证：</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">通过代码检查工具（比如Linter）验证输出是否为合法的Python代</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">码；</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">执行生成的Python代码，以检验其运行状态；</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">借助其他现代编程大模型，创建单元测试来验证预期功能（这类模型</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">自身无须具备推理能力）；</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">我们甚至可以进一步测量执行时间，在训练过程中优先选择性能更好</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">的解决方案——即使其他方案同样能解决问题。</span>

<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">强</span>

![](./图解DeepSeek技术_assets/images/p096_img01.png)



![](./图解DeepSeek技术_assets/images/p096_img02.jpg)

---

## Page 97

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">在训练阶段，我们可以向模型提出此类问题，并生成多个可能的答</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">案，如图3-9所示。</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图3-9</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">在训练阶段生成多个答案</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">通过上述自动验证（无须人工干预），我们可以发现：</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">第一个结果甚至不是有效的代码；</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">第二个结果虽然是代码，但并不是Python语言编写的；</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">第三个结果看似可行，但没有通过单元测试；</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">第四个结果才是完全正确的答案。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">这个自动验证的过程如图3-1
0所示。</span>

![](./图解DeepSeek技术_assets/images/p097_img01.jpg)

---

## Page 98

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图3-10</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">在训练阶段进行自动验证</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">这类反馈信号都可以直接用于模型优化，如图3-1
1所示。训练过程自</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">然需要基于大量的示例（以小批量形式）持续迭代，并通过反复的训</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">练逐步实现。</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图3-11</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">反馈信号用于优化模型</span>

![](./图解DeepSeek技术_assets/images/p098_img01.jpg)



![](./图解DeepSeek技术_assets/images/p098_img02.jpg)

---

## Page 99

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">这些奖励信号与模型参数更新机制，正是驱动模型在强化学习训练过</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">程中持续提升任务表现的核心原理——该原理在DeepSeek-R1论文中</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">亦有直观呈现，如图3-12</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">程，但它可以通过“强化”正确的思考过程，让模型以更高概率生成正确的思考过程和答案。</span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">图中展示的pass@1是指模型一次输出正确答案的概率，而cons@16是指让模型重复输出16</span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">次，取其中最频繁出现的答案，该候选答案正确的概率。cons@16高于pass@1的事实说明，</span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">即使未经强化学习训练的基座模型也有一定的概率输出正确答案。随着训练步数的增加，这些</span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">偶然出现的正确答案会被强化，使答案正确的概率大大提高。——译者注</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">所示。</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">该注释图来自DeepSeek-R1论文。对于每个问题，DeepSeek研发团</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">队随机抽取
16个响应并计算整体平均准确率以确保评估的稳定性。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图3-12</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">反馈信号用于持续优化DeepSeek-R1-Zero</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">该过程使用的强化学习算法是组相对策略优化</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">（group</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">relative</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">policy</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">optimization，GRPO</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span>

<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">强化学习训练不能创造基座模型本来不可能生成的思考过</span>

<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Shao,</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Zhihong,</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">et</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">al.</span>

![](./图解DeepSeek技术_assets/images/p099_img01.png)



![](./图解DeepSeek技术_assets/images/p099_img02.jpg)



![](./图解DeepSeek技术_assets/images/p099_img03.png)

---

## Page 100

<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">“DeepSeekMath:</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Pushing</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">the</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Limits</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">of</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Mathematical</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Reasoning</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">in</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Open</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Language</span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">Models.”arXiv</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">preprint</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">arXiv:2402.03300</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">(2024).</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">）。GRPO算法的核心思想在</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">于：它会根据结果调整那些导致正确答案或错误答案的决策路径的概</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">率权重。这些决策既包括词元的选择组合，也包含具体的推理步骤。</span>
<span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000">3.2.2</span><span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000">DeepSeek-R1-Zero的完整训练过程</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">DeepSeek-R1-Zero的完整训练过程始于我们先前探讨的类系统提示</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">词。该模型以DeepSeek-V3-Base为基础模型，让其生成推理过程</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">（位于&lt;think&gt;与&lt;/think&gt;标签之间）和答案（位于&lt;answer&gt;与</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">&lt;/answer&gt;标签之间）。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">不仅生成的推理和答案会根据&lt;think&gt;和&lt;answer&gt;标签的使用情况进</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">行验证，而且答案还会通过结构化方法进行评估。我们之前讨论过，</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">这可能涉及通过执行Python代码判断输出是否正确。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">一如强化学习中的典型做法，训练过程会迭代进行，直到模型收敛。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">最终，我们得到了DeepSeek-R1-Zero，如图3-1
3所示。</span>

---

## Page 101

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图3-13</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">DeepSeek-R1-Zero</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">值得注意的是，我们并不需要举例说明&lt;think&gt;处理过程的具体形式，</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">仅指出“模型应使用&lt;think&gt;标签”就足够了！</span>

![](./图解DeepSeek技术_assets/images/p101_img01.jpg)

---

## Page 102

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">通过设计一种间接奖励机制，鼓励模型表现出类似于思维链的行为，</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">模型自行领悟到：推理过程越长、越复杂，答案正确的概率就越高，</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">如图3-1
4所示。</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">该注释图来自论文“DeepSeek-</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">R1:</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">Incentivizing</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">Reasoning</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">Capability</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">in</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">LLMs</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">via</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">Reinforcem</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">ent</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">Learning”。通过间接的强化学习奖励机制，模型能够持续增加</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">推理步数，从而自由探索最优的类思维链行为。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图3-14</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">DeepSeek-R1-Zero的推理训练</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图3-1
4的重要性在于它进一步凸显了从训练时计算到测试时计算的范</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">式转变。随着模型生成更长的思维序列，其重点逐渐转向测试时计</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">算。</span>

![](./图解DeepSeek技术_assets/images/p102_img01.jpg)

---

## Page 103

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">使用该训练流程，研究团队发现模型能自主发现最优的类思维链行</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">为，包括自我反思（self-reflection）和自我验证（self-</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">verification）等高级推理能力。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">虽然这一流程有效，DeepSeek-R1-Zero模型在推理问题上也得分很</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">高，但仍存在一些其他问题使其可用性不如预期。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">尽管DeepSeek-R1-Zero展现出强大的推理能力，并能自主发展出意想</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">不到的强大推理行为，但它也面临一些问题，例如，DeepSeek-R1-</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">Zero存在生成结果可读性差和语言混杂等显著缺陷。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">因此，研究团队最终转向了另一个方案——如今广为人知的DeepSeek-</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">R1。在继续探讨DeepSeek-R1之前，我们需要先了解其基础模型</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">DeepSeek-V3采用的效率优化策略。</span>

---

## Page 104

<span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000">3.3</span><span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000">DeepSeek-V3的效率优化策略</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">DeepSeek-R1的核心优势在很大程度上源于其基础模型DeepSeek-V3</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">——毕竟，DeepSeek-R1本质上就是DeepSeek-V3的微调版本。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">因此，我们需要重点解析DeepSeek-V3实现高效训练的三大关键优化</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">策略：</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">多头潜在注意力机制</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">混合精度训练</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">多词元预测</span>
<span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000">3.3.1</span><span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000">多头潜在注意力机制</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">如先前所述，多头潜在注意力（multi-head</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">latent</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">attention，</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">MLA）机制通过对键（K）和值（V）进行低秩联合压缩，显著减少了</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">推理过程中KV缓存的开销。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">该方法是对传统多头注意力机制的改良（大部分LLM都采用传统多头</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">注意力机制）。在传统多头注意力机制中，每个注意力头维护独立的</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">Q/K/V（查询/键/值）权重矩阵，从而产生不同的Q/K/V矩阵的表示。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">传统多头注意力机制如图3-1
5所示。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">在传统多头注意力机制（及多数注意力变体）中，其键和值通常会被</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">缓存。通过缓存已生成词元的键和值，模型只需专注于计算新词元的</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">注意力权重。</span>

---

## Page 105

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图3-15</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">传统多头注意力机制</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">虽然KV缓存机制效率显著，但当输入序列较长时，缓存会迅速膨胀并</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">消耗大量内存，导致出现计算瓶颈。而多头潜在注意力机制将Q以及</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">KV先压缩成低秩表示，从而使KV缓存更加高效，如图3-1
6所示。</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图3-16</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">多头潜在注意力机制将Q、KV压缩成低秩表示</span>

![](./图解DeepSeek技术_assets/images/p105_img01.jpg)



![](./图解DeepSeek技术_assets/images/p105_img02.jpg)

---

## Page 106

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">压缩处理后，系统会缓存这些键</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">-</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">值向量（此时称为潜在KV），用于</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">后续新词元的注意力矩阵计算。之所以称为“潜在KV”，是因为其本</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">质是原始高维数据的低维表征。在具体使用时，这些缓存的潜在KV矩</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">阵会通过投影变换恢复至原始的模型尺寸，如图3-1
7所示。</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图3-17</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">通过投影变换恢复至原始的模型尺寸</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">需要注意的是，独立的键向量会应用位置嵌入（RoPE，旋转位置嵌</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">入）处理并同样进行缓存以加速推理。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">最终，Q/K/V值会像传统多头注意力机制一样进行组合和处理，如图3-</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">18所示。</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图3-18</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">像传统注意力机制一样进行组合和处理</span>

![](./图解DeepSeek技术_assets/images/p106_img01.jpg)



![](./图解DeepSeek技术_assets/images/p106_img02.jpg)

---

## Page 107

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">这种方法不仅避免了对已生成词元的重复计算，而且比以往的方法</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">（如分组查询注意力）更加高效。</span>
<span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000">3.3.2</span><span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000">混合精度训练</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">为了进一步提升DeepSeek-V3在训练和推理过程中的效率，</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">DeepSeek团队提出了使用FP8格式进行混合精度（mixed-</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">precision）训练的方法。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">数值精度（precision</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">of</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">value）指的是以尽可能少的位数来表示一个</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">给定数值的能力，如图3-1
9所示。</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图3-19</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">数值精度</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">使用低精度数据格式时需要注意，它可表示的值的范围会随着精度的</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">降低而减小，如图3-2
0所示。</span>

![](./图解DeepSeek技术_assets/images/p107_img01.jpg)

---

## Page 108

<span style="font-family:'Unnamed-T3';font-size:14.83pt;co