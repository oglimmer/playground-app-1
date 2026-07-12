import { describe, expect, it } from 'vitest'
import { renderMarkdown } from './markdown'

describe('renderMarkdown', () => {
  it('renders headings and emphasis', () => {
    const html = renderMarkdown('# Title\n\nsome **bold** text')
    expect(html).toContain('<h1')
    expect(html).toContain('Title')
    expect(html).toContain('<strong>bold</strong>')
  })

  it('renders fenced code blocks', () => {
    const html = renderMarkdown('```\nconst x = 1\n```')
    expect(html).toContain('<pre>')
    expect(html).toContain('const x = 1')
  })

  it('strips dangerous script tags', () => {
    const html = renderMarkdown('hello <script>alert(1)</script> world')
    expect(html).not.toContain('<script>')
    expect(html).toContain('hello')
  })

  it('drops javascript: links', () => {
    const html = renderMarkdown('[click](javascript:alert(1))')
    expect(html).not.toContain('javascript:')
  })

  it('handles empty input without throwing', () => {
    expect(renderMarkdown('')).toBe('')
  })
})

describe('wiki page links', () => {
  it('converts /PageName to a clickable link', () => {
    const html = renderMarkdown('see /test for details')
    expect(html).toContain('<a href="/pages/test">/test</a>')
  })

  it('handles multiple links in one line', () => {
    const html = renderMarkdown('/foo and /bar')
    expect(html).toContain('<a href="/pages/foo">/foo</a>')
    expect(html).toContain('<a href="/pages/bar">/bar</a>')
  })

  it('handles link at the start of a line', () => {
    const html = renderMarkdown('/test is a page')
    expect(html).toContain('<a href="/pages/test">/test</a>')
  })

  it('handles hyphenated page names', () => {
    const html = renderMarkdown('see /my-cool-page here')
    expect(html).toContain('<a href="/pages/my-cool-page">/my-cool-page</a>')
  })

  it('handles underscore in page names', () => {
    const html = renderMarkdown('see /my_page here')
    expect(html).toContain('<a href="/pages/my_page">/my_page</a>')
  })

  it('does not match inside http:// URLs', () => {
    const html = renderMarkdown('visit http://example.com')
    expect(html).not.toContain('/pages/example')
    expect(html).not.toContain('/pages/example.com')
  })

  it('does not match inside https:// URLs', () => {
    const html = renderMarkdown('visit https://example.com/path')
    expect(html).not.toContain('/pages/path')
  })

  it('does not match inside existing markdown links', () => {
    const html = renderMarkdown('[click here](/page-name)')
    expect(html).toContain('<a href="/page-name">click here</a>')
    // Should not double-process the destination
    expect(html).not.toContain('/pages/page-name')
  })

  it('does not match /PageName inside inline link text', () => {
    const html = renderMarkdown('[go /foo](https://example.com)')
    // The original link should be preserved — no nested /pages/ link injected
    expect(html).not.toContain('/pages/foo')
    expect(html).toContain('https://example.com')
  })

  it('does not match /PageName inside reference link text', () => {
    const html = renderMarkdown('[see /bar][1]\n\n[1]: https://example.com')
    expect(html).not.toContain('/pages/bar')
  })

  it('does not process /PageName inside inline code', () => {
    const html = renderMarkdown('use `/api` to call')
    expect(html).toContain('<code>/api</code>')
    expect(html).not.toContain('/pages/api')
  })

  it('does not process /PageName inside code blocks', () => {
    const html = renderMarkdown('```\n/api\n```')
    expect(html).toContain('<pre>')
    expect(html).toContain('/api')
    // The protected code block should not have the link
    expect(html).not.toContain('/pages/api')
  })

  it('links /PageName at the end of a sentence', () => {
    const html = renderMarkdown('see /test.')
    expect(html).toContain('<a href="/pages/test">/test</a>')
  })
})
