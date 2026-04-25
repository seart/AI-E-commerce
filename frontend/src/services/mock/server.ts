import { ENV } from '@/config/env'
import { STORAGE_KEYS } from '@/constants/storage'
import { clearStoredSession, getStoredSession, setStoredSession } from '@/services/session'
import { loadJson, saveJson } from '@/utils/storage'
import {
  defaultProfileStats,
  mockBanners,
  mockCategories,
  mockMerchants,
  mockProducts,
} from './assets'
import type {
  Address,
  AddressInput,
  CartItem,
  CheckoutItem,
  HomePageData,
  LoginPayload,
  Merchant,
  MerchantDetail,
  Order,
  OrderLine,
  PaymentChannel,
  PaymentPrepayResponse,
  PaymentStatusResponse,
  Product,
  RegisterPayload,
  UserProfile,
  UserProfileStats,
  UserSession,
} from '@/types/domain'

interface MockUserRecord {
  id: string
  mobile: string
  password: string
  nickname: string
  memberLevel: string
  profileStats: UserProfileStats
}

interface MockDatabase {
  users: MockUserRecord[]
  addressesByUserId: Record<string, Address[]>
  cartsByUserId: Record<string, CartItem[]>
  ordersByUserId: Record<string, Order[]>
  paymentsById: Record<string, PaymentPrepayResponse & { settleAfter: string }>
}

function uid(prefix: string) {
  return `${prefix}_${crypto.randomUUID().replace(/-/g, '').slice(0, 12)}`
}

function nowIso() {
  return new Date().toISOString()
}

function wait<T>(value: T, delay = ENV.enableMock ? 180 : 0) {
  return new Promise<T>((resolve) => {
    window.setTimeout(() => resolve(value), delay)
  })
}

function createSeedDatabase(): MockDatabase {
  const demoUserId = 'u_demo'

  return {
    users: [
      {
        id: demoUserId,
        mobile: '13800000000',
        password: '123456',
        nickname: '企业采购员',
        memberLevel: 'PLUS企业会员',
        profileStats: defaultProfileStats,
      },
    ],
    addressesByUserId: {
      [demoUserId]: [
        {
          id: 'addr_demo_1',
          city: '北京市',
          district: '朝阳区',
          street: '大望路商务区',
          detail: 'SOHO现代城 A 座 10 层 1008',
          contactName: '张采购',
          phone: '13800000000',
          tag: '公司',
          isDefault: true,
        },
      ],
    },
    cartsByUserId: {
      [demoUserId]: [],
    },
    ordersByUserId: {
      [demoUserId]: [],
    },
    paymentsById: {},
  }
}

function readDatabase() {
  const database = loadJson<MockDatabase>(STORAGE_KEYS.mockDatabase, createSeedDatabase())
  database.paymentsById = database.paymentsById ?? {}
  return database
}

function writeDatabase(database: MockDatabase) {
  saveJson(STORAGE_KEYS.mockDatabase, database)
}

function getCurrentUserRecord(database: MockDatabase) {
  const session = getStoredSession()
  const userId = session?.user.id ?? 'u_demo'
  const record = database.users.find((item) => item.id === userId)

  if (!record) {
    throw new Error('当前登录态无效，请重新登录')
  }

  return record
}

function buildSession(user: MockUserRecord): UserSession {
  return {
    accessToken: `mock-token-${user.id}`,
    refreshToken: `mock-refresh-${user.id}`,
    expiresAt: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000).toISOString(),
    user: {
      id: user.id,
      mobile: user.mobile,
      nickname: user.nickname,
      memberLevel: user.memberLevel,
    },
  }
}

function findMerchantOrThrow(merchantId: string) {
  const merchant = mockMerchants.find((item) => item.id === merchantId)
  if (!merchant) {
    throw new Error('商家不存在或已下线')
  }
  return merchant
}

function findProductOrThrow(productId: string) {
  const product = mockProducts.find((item) => item.id === productId)
  if (!product) {
    throw new Error('商品不存在或已下架')
  }
  return product
}

function getCart(database: MockDatabase, userId: string) {
  return database.cartsByUserId[userId] ?? []
}

function getAddresses(database: MockDatabase, userId: string) {
  return database.addressesByUserId[userId] ?? []
}

function getOrders(database: MockDatabase, userId: string) {
  return database.ordersByUserId[userId] ?? []
}

function normalizeAddresses(addresses: Address[]) {
  if (addresses.length === 0) {
    return addresses
  }

  const hasDefault = addresses.some((item) => item.isDefault)
  if (hasDefault) {
    return addresses
  }

  return addresses.map((item, index) => ({
    ...item,
    isDefault: index === 0,
  }))
}

function toOrderLine(item: CartItem): OrderLine {
  return {
    ...item,
    amount: Number((item.price * item.quantity).toFixed(2)),
  }
}

export const mockServer = {
  async login(payload: LoginPayload) {
    const database = readDatabase()
    const user = database.users.find((item) => item.mobile === payload.mobile)

    if (!user || user.password !== payload.password) {
      throw new Error('手机号或密码错误')
    }

    const session = buildSession(user)
    setStoredSession(session)

    return wait(session)
  },

  async register(payload: RegisterPayload) {
    const database = readDatabase()

    if (database.users.some((item) => item.mobile === payload.mobile)) {
      throw new Error('该手机号已注册')
    }

    const user: MockUserRecord = {
      id: uid('u'),
      mobile: payload.mobile,
      password: payload.password,
      nickname: `用户${payload.mobile.slice(-4)}`,
      memberLevel: '普通会员',
      profileStats: defaultProfileStats,
    }

    database.users.unshift(user)
    database.addressesByUserId[user.id] = []
    database.cartsByUserId[user.id] = []
    database.ordersByUserId[user.id] = []
    writeDatabase(database)

    return wait({ success: true })
  },

  async logout() {
    clearStoredSession()
    return wait({ success: true })
  },

  async getHomePage(): Promise<HomePageData> {
    return wait({
      banners: mockBanners,
      categories: mockCategories,
      featuredMerchants: mockMerchants,
    })
  },

  async searchMerchants(keyword: string) {
    const normalized = keyword.trim().toLowerCase()

    if (!normalized) {
      return wait(mockMerchants)
    }

    const merchants = mockMerchants.filter((merchant) => {
      const hitMerchant = [
        merchant.name,
        merchant.description,
        merchant.notice,
        merchant.tags.join(' '),
      ]
        .join(' ')
        .toLowerCase()
        .includes(normalized)

      const hitProduct = mockProducts.some(
        (product) =>
          product.merchantId === merchant.id &&
          [product.name, product.description].join(' ').toLowerCase().includes(normalized),
      )

      return hitMerchant || hitProduct
    })

    return wait(merchants)
  },

  async getMerchantDetail(merchantId: string): Promise<MerchantDetail> {
    const merchant = findMerchantOrThrow(merchantId)
    const products = mockProducts.filter((item) => item.merchantId === merchantId)

    return wait({
      merchant,
      products,
    })
  },

  async getCartItems() {
    const database = readDatabase()
    const user = getCurrentUserRecord(database)
    return wait([...getCart(database, user.id)])
  },

  async addCartItem(productId: string) {
    const database = readDatabase()
    const user = getCurrentUserRecord(database)
    const cartItems = getCart(database, user.id)
    const existing = cartItems.find((item) => item.id === productId)

    if (existing) {
      existing.quantity += 1
    } else {
      const product = findProductOrThrow(productId)
      cartItems.push({
        ...product,
        quantity: 1,
        checked: true,
      })
    }

    const nextCartItems = [...cartItems]
    database.cartsByUserId[user.id] = nextCartItems
    writeDatabase(database)

    return wait(nextCartItems)
  },

  async updateCartItemQuantity(productId: string, quantity: number) {
    const database = readDatabase()
    const user = getCurrentUserRecord(database)
    const cartItems = getCart(database, user.id)
    const index = cartItems.findIndex((item) => item.id === productId)

    if (index < 0) {
      throw new Error('购物车商品不存在')
    }

    if (quantity <= 0) {
      cartItems.splice(index, 1)
    } else {
      const target = cartItems[index]
      if (!target) {
        throw new Error('购物车商品不存在')
      }
      target.quantity = quantity
    }

    const nextCartItems = [...cartItems]
    database.cartsByUserId[user.id] = nextCartItems
    writeDatabase(database)

    return wait(nextCartItems)
  },

  async setCartItemChecked(productId: string, checked: boolean) {
    const database = readDatabase()
    const user = getCurrentUserRecord(database)
    const cartItems = getCart(database, user.id)
    const target = cartItems.find((item) => item.id === productId)

    if (!target) {
      throw new Error('购物车商品不存在')
    }

    target.checked = checked
    const nextCartItems = [...cartItems]
    database.cartsByUserId[user.id] = nextCartItems
    writeDatabase(database)

    return wait(nextCartItems)
  },

  async setMerchantCartChecked(merchantId: string, checked: boolean) {
    const database = readDatabase()
    const user = getCurrentUserRecord(database)
    const cartItems = getCart(database, user.id)

    const nextCartItems = cartItems.map((item) =>
      item.merchantId === merchantId ? { ...item, checked } : item,
    )
    database.cartsByUserId[user.id] = nextCartItems
    writeDatabase(database)

    return wait(nextCartItems)
  },

  async setAllCartChecked(checked: boolean) {
    const database = readDatabase()
    const user = getCurrentUserRecord(database)
    const cartItems = getCart(database, user.id)

    const nextCartItems = cartItems.map((item) => ({
      ...item,
      checked,
    }))
    database.cartsByUserId[user.id] = nextCartItems
    writeDatabase(database)

    return wait(nextCartItems)
  },

  async clearCheckedCartItems() {
    const database = readDatabase()
    const user = getCurrentUserRecord(database)
    const cartItems = getCart(database, user.id)

    const nextCartItems = cartItems.filter((item) => !item.checked)
    database.cartsByUserId[user.id] = nextCartItems
    writeDatabase(database)

    return wait(nextCartItems)
  },

  async getAddresses() {
    const database = readDatabase()
    const user = getCurrentUserRecord(database)
    const addresses = normalizeAddresses(getAddresses(database, user.id))
    database.addressesByUserId[user.id] = addresses
    writeDatabase(database)

    return wait(addresses)
  },

  async createAddress(payload: AddressInput) {
    const database = readDatabase()
    const user = getCurrentUserRecord(database)
    const current = getAddresses(database, user.id)
    const nextAddress: Address = {
      id: uid('addr'),
      ...payload,
    }

    const addresses = payload.isDefault
      ? current.map((item) => ({ ...item, isDefault: false })).concat(nextAddress)
      : current.concat(nextAddress)

    database.addressesByUserId[user.id] = normalizeAddresses(addresses)
    writeDatabase(database)

    return wait(nextAddress)
  },

  async updateAddress(addressId: string, payload: AddressInput) {
    const database = readDatabase()
    const user = getCurrentUserRecord(database)
    const current = getAddresses(database, user.id)
    const index = current.findIndex((item) => item.id === addressId)

    if (index < 0) {
      throw new Error('地址不存在')
    }

    const next = current.map((item) =>
      payload.isDefault ? { ...item, isDefault: false } : item,
    )
    next[index] = {
      id: addressId,
      ...payload,
    }

    database.addressesByUserId[user.id] = normalizeAddresses(next)
    writeDatabase(database)

    return wait(next[index])
  },

  async getOrders() {
    const database = readDatabase()
    const user = getCurrentUserRecord(database)
    const orders = [...getOrders(database, user.id)].sort((left, right) =>
      right.createdAt.localeCompare(left.createdAt),
    )

    return wait(orders)
  },

  async createOrder(addressId: string, items: CheckoutItem[]) {
    const database = readDatabase()
    const user = getCurrentUserRecord(database)
    const addresses = getAddresses(database, user.id)
    const address = addresses.find((item) => item.id === addressId)

    if (!address) {
      throw new Error('请选择有效的收货地址')
    }

    const cartItems = getCart(database, user.id)
    const lines = items.map(({ productId, quantity }) => {
      const source =
        cartItems.find((item) => item.id === productId) ?? findProductOrThrow(productId)

      return toOrderLine({
        ...source,
        quantity,
        checked: true,
      })
    })

    const order: Order = {
      id: uid('order'),
      orderNo: `JD${Date.now()}`,
      createdAt: nowIso(),
      totalAmount: Number(lines.reduce((sum, item) => sum + item.amount, 0).toFixed(2)),
      status: 'PENDING_PAYMENT',
      statusText: '待支付',
      paymentStatus: 'PENDING',
      paymentChannel: null,
      paidAt: null,
      paymentExpireAt: new Date(Date.now() + 15 * 60 * 1000).toISOString(),
      closedAt: null,
      items: lines,
      address,
    }

    const checkedIds = new Set(items.map((item) => item.productId))
    database.ordersByUserId[user.id] = [order, ...getOrders(database, user.id)]
    database.cartsByUserId[user.id] = cartItems.filter((item) => !checkedIds.has(item.id))
    writeDatabase(database)

    return wait(order)
  },

  async prepay(orderId: string, channel: PaymentChannel) {
    const database = readDatabase()
    const user = getCurrentUserRecord(database)
    const order = getOrders(database, user.id).find((item) => item.id === orderId)

    if (!order || order.status !== 'PENDING_PAYMENT') {
      throw new Error('订单状态不允许发起支付')
    }

    const payment: PaymentPrepayResponse & { settleAfter: string } = {
      paymentId: uid('pay'),
      orderId,
      channel,
      status: 'PAYING',
      amount: order.totalAmount,
      outTradeNo: `MOCK${Date.now()}`,
      transactionId: null,
      qrContent: `mock-pay://${channel}/${order.orderNo}`,
      expireAt: order.paymentExpireAt ?? new Date(Date.now() + 15 * 60 * 1000).toISOString(),
      settleAfter: new Date(Date.now() + 2500).toISOString(),
    }

    order.paymentStatus = 'PAYING'
    order.paymentChannel = channel
    database.paymentsById[payment.paymentId] = payment
    writeDatabase(database)

    return wait(payment)
  },

  async getPaymentStatus(paymentId: string): Promise<PaymentStatusResponse> {
    const database = readDatabase()
    const user = getCurrentUserRecord(database)
    const payment = database.paymentsById[paymentId]

    if (!payment) {
      throw new Error('支付单不存在')
    }

    const order = getOrders(database, user.id).find((item) => item.id === payment.orderId)
    if (!order) {
      throw new Error('订单不存在')
    }

    if (payment.status === 'PAYING' && Date.now() >= new Date(payment.settleAfter).getTime()) {
      payment.status = 'PAID'
      payment.transactionId = `mock_tx_${Date.now()}`
      order.status = 'PAID'
      order.statusText = '支付成功'
      order.paymentStatus = 'PAID'
      order.paidAt = nowIso()
      writeDatabase(database)
    }

    return wait({
      paymentId: payment.paymentId,
      orderId: payment.orderId,
      channel: payment.channel,
      status: payment.status,
      orderStatus: order.status,
      transactionId: payment.transactionId,
      expireAt: payment.expireAt,
      paidAt: order.paidAt,
      closedAt: order.closedAt,
    })
  },

  async cancelOrder(orderId: string, _reason: string) {
    const database = readDatabase()
    const user = getCurrentUserRecord(database)
    const order = getOrders(database, user.id).find((item) => item.id === orderId)

    if (!order || order.status !== 'PENDING_PAYMENT') {
      throw new Error('订单状态不允许取消')
    }

    order.status = 'PAYMENT_CLOSED'
    order.statusText = '支付关闭'
    order.paymentStatus = 'CLOSED'
    order.closedAt = nowIso()
    writeDatabase(database)

    return wait(order)
  },

  async requestRefund(orderId: string, _reason: string) {
    const database = readDatabase()
    const user = getCurrentUserRecord(database)
    const order = getOrders(database, user.id).find((item) => item.id === orderId)

    if (!order || !['PAID', 'PREPARING', 'DELIVERING', 'COMPLETED'].includes(order.status)) {
      throw new Error('订单状态不允许申请退款')
    }

    order.status = 'REFUND_REQUESTED'
    order.statusText = '退款申请中'
    writeDatabase(database)

    return wait(order)
  },

  async getProfile(): Promise<UserProfile> {
    const database = readDatabase()
    const user = getCurrentUserRecord(database)

    return wait({
      id: user.id,
      nickname: user.nickname,
      mobile: user.mobile,
      avatarText: user.nickname.slice(0, 1),
      memberLevel: user.memberLevel,
      stats: user.profileStats,
    })
  },

  async resolveMerchantsByIds(merchantIds: string[]): Promise<Merchant[]> {
    const uniqueIds = new Set(merchantIds)
    return wait(mockMerchants.filter((item) => uniqueIds.has(item.id)))
  },

  async resolveProductsByIds(productIds: string[]): Promise<Product[]> {
    const uniqueIds = new Set(productIds)
    return wait(mockProducts.filter((item) => uniqueIds.has(item.id)))
  },
}
