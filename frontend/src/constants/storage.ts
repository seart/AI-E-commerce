// localStorage key 统一维护，避免不同模块写出多个不兼容的缓存键。
export const STORAGE_KEYS = {
  session: 'jingdong-enterprise/session',
  mockDatabase: 'jingdong-enterprise/mock-db',
  searchHistory: 'jingdong-enterprise/search-history',
}
