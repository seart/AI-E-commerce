import imgChaoshan from '@/assets/超市.png'
import imgCaishi from '@/assets/菜市场.png'
import imgShuiguo from '@/assets/水果店.png'
import imgXianhua from '@/assets/鲜花.png'
import imgYiyao from '@/assets/医药健康.png'
import imgJiaju from '@/assets/家居.png'
import imgDangao from '@/assets/蛋糕.png'
import imgQiandao from '@/assets/签到.png'
import imgDapai from '@/assets/大牌免运.png'
import imgHongbao from '@/assets/红包.png'
import type { Banner, Category, Merchant, Product, UserProfileStats } from '@/types/domain'

export const mockCategories: Category[] = [
  { id: 'supermarket', name: '超市便利', icon: imgChaoshan },
  { id: 'market', name: '菜市场', icon: imgCaishi },
  { id: 'fruit', name: '水果店', icon: imgShuiguo },
  { id: 'flower', name: '鲜花绿植', icon: imgXianhua },
  { id: 'health', name: '医药健康', icon: imgYiyao },
  { id: 'home', name: '家居时尚', icon: imgJiaju },
  { id: 'cake', name: '烘焙蛋糕', icon: imgDangao },
  { id: 'checkin', name: '签到', icon: imgQiandao },
  { id: 'brand', name: '大牌免运', icon: imgDapai },
  { id: 'coupon', name: '红包套餐', icon: imgHongbao },
]

export const mockBanners: Banner[] = [
  {
    id: 'b1',
    title: '京东秒送企业版',
    subtitle: '全城优选商家，1小时安心达',
    background: 'linear-gradient(135deg, #ff7a18 0%, #e1251b 100%)',
  },
  {
    id: 'b2',
    title: '生鲜直降专区',
    subtitle: '满99减20，会员叠加券更划算',
    background: 'linear-gradient(135deg, #00b894 0%, #00cec9 100%)',
  },
]

export const mockMerchants: Merchant[] = [
  {
    id: 'm1',
    name: '沃尔玛精选超市',
    monthlySales: 12890,
    minOrderAmount: 0,
    deliveryFee: 5,
    etaMinutes: 28,
    tags: ['满99减20', '会员88折', '极速退款'],
    description: '品牌商超，覆盖生鲜百货与日常补给',
    notice: '平台严选商家，支持预约配送与电子发票',
    rating: 4.9,
    heroColor: 'linear-gradient(180deg, #1f9af8 0%, #0e85f0 100%)',
    logoText: '沃尔',
    categories: [
      { id: 'fresh', name: '新鲜水果' },
      { id: 'seafood', name: '海鲜水产' },
      { id: 'snack', name: '休闲零食' },
      { id: 'drink', name: '酒水饮料' },
    ],
  },
  {
    id: 'm2',
    name: '永辉品质生活馆',
    monthlySales: 8620,
    minOrderAmount: 0,
    deliveryFee: 0,
    etaMinutes: 35,
    tags: ['首单立减', '0元配送', '次日赔付'],
    description: '社区高频消费精选，配送时效稳定',
    notice: '晚间订单支持次晨优先送达',
    rating: 4.8,
    heroColor: 'linear-gradient(180deg, #00b894 0%, #00997a 100%)',
    logoText: '永辉',
    categories: [
      { id: 'fresh', name: '新鲜水果' },
      { id: 'snack', name: '休闲零食' },
      { id: 'drink', name: '酒水饮料' },
    ],
  },
  {
    id: 'm3',
    name: '山姆全球精选',
    monthlySales: 6510,
    minOrderAmount: 59,
    deliveryFee: 12,
    etaMinutes: 48,
    tags: ['全球好物', '企业专享', '高端会员价'],
    description: '大包装囤货好物，家庭与企业采购优选',
    notice: '支持企业采购单与专属客服',
    rating: 4.9,
    heroColor: 'linear-gradient(180deg, #2d3436 0%, #636e72 100%)',
    logoText: '山姆',
    categories: [
      { id: 'fresh', name: '新鲜水果' },
      { id: 'seafood', name: '海鲜水产' },
      { id: 'drink', name: '酒水饮料' },
    ],
  },
]

export const mockProducts: Product[] = [
  {
    id: 'p1',
    merchantId: 'm1',
    merchantName: '沃尔玛精选超市',
    categoryId: 'fresh',
    name: '泰国进口金枕榴莲果肉 300g',
    sales: 342,
    price: 39.9,
    originalPrice: 59.9,
    imageText: '榴莲',
    unit: '份',
    description: '冷链到仓，新鲜果肉即开即食',
    stock: 180,
  },
  {
    id: 'p2',
    merchantId: 'm1',
    merchantName: '沃尔玛精选超市',
    categoryId: 'fresh',
    name: '新疆阿克苏冰糖心苹果 1kg',
    sales: 1205,
    price: 19.9,
    originalPrice: 29.9,
    imageText: '苹果',
    unit: '袋',
    description: '脆甜多汁，适合家庭囤货',
    stock: 320,
  },
  {
    id: 'p3',
    merchantId: 'm1',
    merchantName: '沃尔玛精选超市',
    categoryId: 'drink',
    name: '农夫山泉饮用天然水 550ml*12',
    sales: 4500,
    price: 13.9,
    originalPrice: 15.9,
    imageText: '矿泉水',
    unit: '箱',
    description: '企业团购高频补货品',
    stock: 860,
  },
  {
    id: 'p4',
    merchantId: 'm2',
    merchantName: '永辉品质生活馆',
    categoryId: 'snack',
    name: '散装卫龙甜面筋 200g',
    sales: 500,
    price: 8.5,
    originalPrice: 10,
    imageText: '辣条',
    unit: '袋',
    description: '爆款休闲零食，办公室常备',
    stock: 540,
  },
  {
    id: 'p5',
    merchantId: 'm2',
    merchantName: '永辉品质生活馆',
    categoryId: 'drink',
    name: '元气森林白桃气泡水 480ml*6',
    sales: 880,
    price: 24.9,
    originalPrice: 29.9,
    imageText: '气泡水',
    unit: '提',
    description: '低糖轻饮，活动福利专区',
    stock: 240,
  },
  {
    id: 'p6',
    merchantId: 'm3',
    merchantName: '山姆全球精选',
    categoryId: 'seafood',
    name: '挪威三文鱼切片 400g',
    sales: 260,
    price: 69.9,
    originalPrice: 79.9,
    imageText: '三文鱼',
    unit: '盒',
    description: '冷链直送，高蛋白轻食选择',
    stock: 120,
  },
  {
    id: 'p7',
    merchantId: 'm3',
    merchantName: '山姆全球精选',
    categoryId: 'drink',
    name: '星巴克拿铁咖啡 270ml*10',
    sales: 660,
    price: 56.9,
    originalPrice: 65.9,
    imageText: '咖啡',
    unit: '箱',
    description: '商务茶歇高频采购商品',
    stock: 230,
  },
]

export const defaultProfileStats: UserProfileStats = {
  redPackets: 6,
  coupons: 12,
  points: 2680,
  credit: 5000,
}
