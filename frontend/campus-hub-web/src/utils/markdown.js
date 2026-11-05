/**
 * 极简 Markdown 渲染（先转义再套标签，天然防 XSS）。
 * 只支持对话里真正会用到的语法：标题、无序列表、代码块、行内代码、粗体。
 * 不支持链接自动识别——避免把用户输入里的 URL 变成可点击的未知地址。
 */

function escapeHtml(source) {
  return String(source).replace(/[&<>"']/g, (char) => {
    const map = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }
    return map[char]
  })
}

export function renderMarkdown(source) {
  if (!source) return ''
  let text = String(source).replace(/\r\n/g, '\n')

  // 先摘出代码块，避免块内的 * _ ` 被当作行内语法
  const blocks = []
  text = text.replace(/```([\w+-]*)\n?([\s\S]*?)```/g, (_, lang, code) => {
    blocks.push(
      `<pre class="md-pre"><code>${escapeHtml(code.replace(/\n$/, ''))}</code></pre>`
    )
    return `\u0000BLOCK${blocks.length - 1}\u0000`
  })

  text = escapeHtml(text)
  text = text.replace(/`([^`\n]+)`/g, '<code class="md-inline-code">$1</code>')
  text = text.replace(/\*\*([^*\n]+)\*\*/g, '<strong>$1</strong>')

  const lines = text.split('\n')
  const output = []
  let listItems = null

  const flushList = () => {
    if (listItems) {
      output.push(`<ul class="md-list">${listItems.join('')}</ul>`)
      listItems = null
    }
  }

  for (const rawLine of lines) {
    const line = rawLine.trimEnd()
    const heading = /^(#{1,4})\s+(.*)$/.exec(line)
    const bullet = /^[-*]\s+(.*)$/.exec(line)
    const ordered = /^\d+[.)]\s+(.*)$/.exec(line)

    if (bullet || ordered) {
      if (!listItems) {
        flushList()
        listItems = []
      }
      listItems.push(`<li>${bullet ? bullet[1] : ordered[1]}</li>`)
      continue
    }

    flushList()

    if (heading) {
      const level = Math.min(heading[1].length + 3, 6)
      output.push(`<h${level} class="md-h">${heading[2]}</h${level}>`)
      continue
    }
    if (!line.trim()) {
      output.push('')
      continue
    }
    output.push(`<p>${line}</p>`)
  }
  flushList()

  let html = output.join('\n')
  // 有序列表暂时按无序渲染：对话里的编号多半是随手写的，视觉统一即可
  html = html.replace(/\u0000BLOCK(\d+)\u0000/g, (_, index) => blocks[Number(index)] || '')
  return html
}

export default renderMarkdown
