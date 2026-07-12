import { afterEach, describe, expect, it, vi } from 'vitest'
import { api } from './api'

describe('api.listPages tag filtering', () => {
  afterEach(() => vi.restoreAllMocks())

  it('appends the tag query parameter', async () => {
    const fetchMock = vi
      .spyOn(globalThis, 'fetch')
      .mockResolvedValue(new Response('[]', { status: 200 }))
    await api.listPages('docs')
    expect(fetchMock).toHaveBeenCalledWith('/api/pages?tag=docs', expect.anything())
  })

  it('omits the query parameter when no tag given', async () => {
    const fetchMock = vi
      .spyOn(globalThis, 'fetch')
      .mockResolvedValue(new Response('[]', { status: 200 }))
    await api.listPages()
    expect(fetchMock).toHaveBeenCalledWith('/api/pages', expect.anything())
  })

  it('fetches the tags list endpoint', async () => {
    const fetchMock = vi
      .spyOn(globalThis, 'fetch')
      .mockResolvedValue(new Response('[]', { status: 200 }))
    await api.listTags()
    expect(fetchMock).toHaveBeenCalledWith('/api/tags', expect.anything())
  })
})

describe('api attachments', () => {
  afterEach(() => vi.restoreAllMocks())

  it('lists attachments for a page', async () => {
    const fetchMock = vi
      .spyOn(globalThis, 'fetch')
      .mockResolvedValue(new Response('[]', { status: 200 }))
    await api.listAttachments('my-page')
    expect(fetchMock).toHaveBeenCalledWith('/api/pages/my-page/attachments', expect.anything())
  })

  it('builds the attachment data URL', () => {
    const url = api.attachmentDataUrl('my-page', 'abc-123')
    expect(url).toBe('/api/pages/my-page/attachments/abc-123/data')
  })

  it('deletes an attachment', async () => {
    const fetchMock = vi
      .spyOn(globalThis, 'fetch')
      .mockResolvedValue(new Response(null, { status: 204 }))
    await api.deleteAttachment('my-page', 'abc-123')
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/pages/my-page/attachments/abc-123',
      expect.objectContaining({ method: 'DELETE' }),
    )
  })
})
