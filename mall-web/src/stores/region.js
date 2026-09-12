import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getChildren } from '@/api/region'

/**
 * 行政区划。
 *
 * <p>region-api 只有两个查询接口（按父级取子级、按 id 取单条），没有"给我整棵树"，
 * 所以这里逐层拉取拼出来，进程内缓存一份。种子数据只有 2 省 3 市 1 区，
 * 总共 5 次请求，代价可忽略。</p>
 *
 * <p>另外：`GET /api/regions/id/{id}` 的 SQL <b>没有 join 父级</b>（接口文档写"含父级"，与实现不符），
 * 所以地址里带的 address.name 只有区县名。省市区全名要靠这里的 nameMap 拼。</p>
 */
export const useRegionStore = defineStore('region', () => {
  /** el-cascader 的 options */
  const tree = ref([])
  /** id -> "广东省 / 广州市 / 天河区" */
  const nameMap = ref({})
  const loading = ref(false)
  const loaded = ref(false)

  async function ensureLoaded() {
    if (loaded.value) return tree.value

    loading.value = true
    try {
      const provinces = (await getChildren(0)) || []
      const options = []
      const map = {}

      for (const p of provinces) {
        const cities = (await getChildren(p.id)) || []
        const cityNodes = []

        for (const c of cities) {
          const districts = (await getChildren(c.id)) || []
          map[c.id] = `${p.name} / ${c.name}`

          const cityNode = { value: c.id, label: c.name }
          if (districts.length) {
            cityNode.children = districts.map((d) => {
              map[d.id] = `${p.name} / ${c.name} / ${d.name}`
              return { value: d.id, label: d.name }
            })
          }
          cityNodes.push(cityNode)
        }

        map[p.id] = p.name
        const provNode = { value: p.id, label: p.name }
        // 注意：children 为空数组时 el-cascader 会认为"还有下级"却展不开，
        // 所以没有下级就不要挂 children，让它直接变成叶子节点。
        if (cityNodes.length) provNode.children = cityNodes
        options.push(provNode)
      }

      tree.value = options
      nameMap.value = map
      loaded.value = true
      return options
    } finally {
      loading.value = false
    }
  }

  /** 区划 id → 省市区全名；查不到时返回空串（交给调用方兜底） */
  function labelOf(id) {
    if (id === null || id === undefined) return ''
    return nameMap.value[id] || ''
  }

  return { tree, nameMap, loading, ensureLoaded, labelOf }
})
