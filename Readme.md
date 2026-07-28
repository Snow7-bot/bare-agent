# bare-agent

从零手写的 Java AI Agent，**不依赖任何 Agent 框架**（无 LangChain、无 Spring AI）。

通过 12 天逐步构建，从一行 HTTP 请求到完整的 ReAct 推理循环，深入理解 Agent 底层原理。

## 功能

### 核心能力
- **多轮对话**：System Prompt + 历史记忆，LLM 记住上下文
- **工具调用**：6 个工具（时间、计算器、读文件、写文件、目录列表、模拟搜索）
- **ReAct 循环**：思考 → 行动 → 观察 → 再思考，支持多步推理和自动串联
- **流式输出**：基于 SSE（Server-Sent Events），逐字打印，像 ChatGPT 一样
- **Token 管理**：JTokkit 精确计数 + 滑动窗口裁剪（超 80% 自动裁剪旧消息）
- **结构化提取**：JSON Mode → POJO，从自由文本中提取结构化数据
- **异常处理**：指数退避重试 + 优雅降级，Agent 不崩溃

### 工具列表

| 工具 | 功能 | 参数 |
|------|------|------|
| `get_current_time` | 获取北京时间 | 无 |
| `calculate` | 四则运算 | `expression` |
| `read_file` | 读取文件 | `path` |
| `save_to_file` | 保存文件 | `path`, `content` |
| `list_files` | 列出目录 | `path`（可选） |
| `web_search` | 模拟搜索 | `query` |

## 快速开始

### 环境要求
- JDK 21+
- Maven 3.8+
- DeepSeek API Key（[platform.deepseek.com](https://platform.deepseek.com)）

### 1. 克隆项目
```bash
git clone https://github.com/Snow7-bot/bare-agent.git
cd bare-agent