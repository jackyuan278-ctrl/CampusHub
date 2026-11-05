/**
 * 演示回复：agent 服务未启动 / 接口尚未实现时的兜底，
 * 让前端界面可以先完整跑起来。接上真实服务后，这里可以整文件删掉。
 */

const ABORT_ERROR = () => {
  const error = new Error('aborted')
  error.name = 'AbortError'
  return error
}

function demoChat(text, attachments) {
  const files = attachments?.length
    ? `\n\n我收到了 ${attachments.length} 个附件：${attachments.map((item) => `\`${item.name}\``).join('、')}。`
    : ''
  return `我是 CampusHub 的校园助手，目前跑在**演示模式**（智能体服务还没接上）。

你现在看到的只是界面本身，我本身还没有真正思考。等服务启动后，我大概能帮你做这些事：

- **二手找货**：说出预算和用途，我从在售列表里筛出合适的几件
- **活动推荐**：按兴趣、时间、剩余名额挑活动，并提醒报名截止
- **校园问答**：宿舍、选课、图书馆、办事流程这类问题
- **资料解读**：读你上传的 PDF / 截图，划出重点和考点

你刚才说的是：「${text || '（没有文字，只有附件）'}」。${files}

等服务接上，我会直接调用活动检索和商品查询工具，把真实结果给你。`
}

function demoMarket(text) {
  return `**二手估价参考**（演示模式，非真实成交价）

按成色粗略折价，实际还要看校园里的供需：

- 9 成新、配件齐 → 原价的 **55% ~ 65%**
- 8 成新、正常使用痕迹 → 原价的 **45% ~ 50%**
- 有明显磨损 / 缺配件 → 原价的 **35% ~ 40%**
- 数码类掉价快，季末（毕业季前）能多卖 10% 左右

你描述的是：「${text || '（空）'}」

建议定价时留 10% 的砍价空间，标题里把**品牌 + 型号 + 成色**写全，比写「九成新便宜出」好卖很多。`
}

function demoActivity(text) {
  return `**这周可以关注的活动类型**（演示模式，真实活动等你接上服务再查）

1. **社团招新与体验课** — 一般在周三到周五傍晚，人多、破冰环节多
2. **夜跑 / 羽毛球局** — 报名门槛低，最适合认识新朋友
3. **讲座与工作坊** — 提前 2 天报名，名额通常 30~60 人

你说的需求是：「${text || '（空）'}」

真实场景下，我会去活动服务里按 **时间 + 关键词 + 剩余名额** 过滤，把还能报名的直接列给你，并给出报名入口。`
}

function demoDoc(text, attachments) {
  const files = attachments?.filter((item) => /pdf|image/i.test(`${item.type} ${item.name}`)) || []
  const head = files.length
    ? `收到 ${files.length} 个文件：${files.map((item) => `\`${item.name}\``).join('、')}（演示模式不会真的读内容）`
    : '还没有收到文件——点输入框左下角的「+」上传 PDF 或截图（单个 ≤ 10MB）'
  return `${head}

接上真实服务后，资料解读会走这条链路：

- 解析：PDF 抽文本 / 图片走 OCR
- 切块入库：按段落到向量库，支持长文档
- 回答时**带上原文页码**，你可以点回原处核对

你想让我看哪部分？可以直接说「总结第三章」或者「把表格里的数字列出来」。`
}

export function pickDemoReply({ mode = 'chat', text = '', attachments = [] } = {}) {
  if (mode === 'market') return demoMarket(text)
  if (mode === 'activity') return demoActivity(text)
  if (mode === 'doc') return demoDoc(text, attachments)
  return demoChat(text, attachments)
}

/** 逐字吐出，模拟流式观感（约 14ms / 2 字） */
export function streamDemo(text, { onDelta, signal, speed = 14 } = {}) {
  return new Promise((resolve, reject) => {
    if (signal?.aborted) return reject(ABORT_ERROR())
    let cursor = 0
    const step = 2
    const timer = setInterval(() => {
      if (signal?.aborted) {
        clearInterval(timer)
        reject(ABORT_ERROR())
        return
      }
      cursor = Math.min(cursor + step, text.length)
      onDelta?.(text.slice(Math.max(0, cursor - step), cursor), text.slice(0, cursor))
      if (cursor >= text.length) {
        clearInterval(timer)
        resolve(text)
      }
    }, speed)
  })
}
