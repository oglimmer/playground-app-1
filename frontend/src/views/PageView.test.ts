import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { nextTick } from 'vue'
import { createRouter, createWebHistory } from 'vue-router'
import PageView from './PageView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/pages/:slug', component: PageView },
    { path: '/pages/:slug/edit', component: { template: '<div>edit</div>' } },
    { path: '/', component: { template: '<div>home</div>' } },
  ],
})

describe('PageView attachments', () => {
  let fetchMock: ReturnType<typeof vi.fn>

  beforeEach(() => {
    fetchMock = vi.fn()
    vi.stubGlobal('fetch', fetchMock)
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('renders image attachments as <img> tags with a blob URL', async () => {
    fetchMock.mockImplementation(async (url: string) => {
      if (url === '/api/pages/my-page') {
        return new Response(JSON.stringify({
          slug: 'my-page',
          title: 'Test Page',
          content: 'Hello',
          tags: [],
          attachments: [{ id: 'img-1', filename: 'photo.png', contentType: 'image/png', size: 1024 }],
          createdAt: '2025-01-01T00:00:00Z',
          updatedAt: '2025-01-02T00:00:00Z',
          updatedBy: 'tester',
        }), { status: 200 })
      }
      if (url === '/api/pages/my-page/attachments/img-1/data') {
        return new Response(new Blob([new Uint8Array([0x89, 0x50, 0x4E, 0x47])], { type: 'image/png' }), {
          status: 200, headers: { 'Content-Type': 'image/png' },
        })
      }
      return new Response('{}', { status: 200 })
    })

    const wrapper = mount(PageView, {
      props: { slug: 'my-page' },
      global: { plugins: [router] },
    })

    // Wait for useAsyncData to load and Vue to re-render
    await flushPromises()
    await nextTick()
    // Wait for the blob fetch to complete and Vue to re-render
    await flushPromises()
    await nextTick()

    const img = wrapper.find('img.attachment-image')
    expect(img.exists()).toBe(true)
    expect(img.attributes('src')).toMatch(/^blob:/)
    expect(img.attributes('alt')).toBe('photo.png')
  })

  it('renders non-image attachments as download links', async () => {
    fetchMock.mockImplementation(async (url: string) => {
      if (url === '/api/pages/my-page') {
        return new Response(JSON.stringify({
          slug: 'my-page',
          title: 'Test Page',
          content: 'Hello',
          tags: [],
          attachments: [{ id: 'pdf-1', filename: 'doc.pdf', contentType: 'application/pdf', size: 5120 }],
          createdAt: '2025-01-01T00:00:00Z',
          updatedAt: '2025-01-02T00:00:00Z',
          updatedBy: 'tester',
        }), { status: 200 })
      }
      return new Response('{}', { status: 200 })
    })

    const wrapper = mount(PageView, {
      props: { slug: 'my-page' },
      global: { plugins: [router] },
    })

    await flushPromises()
    await nextTick()

    const link = wrapper.find('a.attachment-file')
    expect(link.exists()).toBe(true)
    expect(link.attributes('href')).toBe('/api/pages/my-page/attachments/pdf-1/data')
    expect(link.text()).toContain('doc.pdf')
    expect(link.text()).toContain('5.0 KB')
  })

  it('shows an error placeholder when an image fails to load', async () => {
    fetchMock.mockImplementation(async (url: string) => {
      if (url === '/api/pages/my-page') {
        return new Response(JSON.stringify({
          slug: 'my-page',
          title: 'Test Page',
          content: 'Hello',
          tags: [],
          attachments: [{ id: 'bad-img', filename: 'broken.jpg', contentType: 'image/jpeg', size: 99 }],
          createdAt: '2025-01-01T00:00:00Z',
          updatedAt: '2025-01-02T00:00:00Z',
          updatedBy: 'tester',
        }), { status: 200 })
      }
      if (url === '/api/pages/my-page/attachments/bad-img/data') {
        return new Response('{"error":"Not found"}', { status: 404, headers: { 'Content-Type': 'application/json' } })
      }
      return new Response('{}', { status: 200 })
    })

    const wrapper = mount(PageView, {
      props: { slug: 'my-page' },
      global: { plugins: [router] },
    })

    await flushPromises()
    await nextTick()
    await flushPromises()
    await nextTick()

    const errorDiv = wrapper.find('.attachment-image-error')
    expect(errorDiv.exists()).toBe(true)
    expect(errorDiv.text()).toBe('Not found')
  })
})
