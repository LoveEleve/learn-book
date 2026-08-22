# Kafka-32 重写规划

> 题目：谁有资格当 leader、怎么投票、怎么提交——KRaft 的 Raft 状态机与 voter 集合主链
> 状态：K-8 KRaft 域第 4 篇，按"Raft 状态机 / voter 集合"展开
> 目标：正面讲透 KRaft 的 Raft 状态机：VoterSet 定义 quorum、EpochElection 过半投票、KafkaRaftClient 处理选主/复制/提交、KRaftControlRecordStateMachine 跟踪 voter 与 kraftVersion 历史。