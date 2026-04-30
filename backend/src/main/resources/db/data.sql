INSERT INTO users (
  id, mobile, password, nickname, member_level,
  coupon_count, favorite_count, points, growth_value
) VALUES (
  'u_demo', '13800000000', '123456', '企业采购员', 'PLUS企业会员',
  6, 12, 2680, 5000
) ON DUPLICATE KEY UPDATE
  password = VALUES(password),
  nickname = VALUES(nickname),
  member_level = VALUES(member_level),
  coupon_count = VALUES(coupon_count),
  favorite_count = VALUES(favorite_count),
  points = VALUES(points),
  growth_value = VALUES(growth_value);

INSERT INTO banners (id, title, subtitle, background, sort_order) VALUES
  ('b1', '京东秒送企业版', '全城优选商家，1小时安心达', 'linear-gradient(135deg, #ff7a18 0%, #e1251b 100%)', 10),
  ('b2', '生鲜直降专区', '满99减20，会员叠加券更划算', 'linear-gradient(135deg, #00b894 0%, #00cec9 100%)', 20)
ON DUPLICATE KEY UPDATE
  title = VALUES(title),
  subtitle = VALUES(subtitle),
  background = VALUES(background),
  sort_order = VALUES(sort_order);

INSERT INTO categories (id, name, icon, sort_order) VALUES
  ('supermarket', '超市便利', '/api/static/category/supermarket.png', 10),
  ('market', '菜市场', '/api/static/category/market.png', 20),
  ('fruit', '水果店', '/api/static/category/fruit.png', 30),
  ('flower', '鲜花绿植', '/api/static/category/flower.png', 40),
  ('health', '医药健康', '/api/static/category/health.png', 50),
  ('home', '家居时尚', '/api/static/category/home.png', 60),
  ('cake', '烘焙蛋糕', '/api/static/category/cake.png', 70),
  ('checkin', '签到', '/api/static/category/checkin.png', 80),
  ('brand', '大牌免运', '/api/static/category/brand.png', 90),
  ('coupon', '红包套餐', '/api/static/category/coupon.png', 100)
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  icon = VALUES(icon),
  sort_order = VALUES(sort_order);

INSERT INTO brands (id, name, logo, description, status, sort_order) VALUES
  ('brand_jd', '京东自营', '', '平台自营品牌', 'ACTIVE', 10)
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  logo = VALUES(logo),
  description = VALUES(description),
  status = VALUES(status),
  sort_order = VALUES(sort_order);

INSERT INTO spec_groups (id, name, status, sort_order) VALUES
  ('spec_capacity', '容量', 'ACTIVE', 10)
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  status = VALUES(status),
  sort_order = VALUES(sort_order);

INSERT INTO spec_options (id, group_id, name, status, sort_order) VALUES
  ('spec_capacity_default', 'spec_capacity', '标准装', 'ACTIVE', 10)
ON DUPLICATE KEY UPDATE
  group_id = VALUES(group_id),
  name = VALUES(name),
  status = VALUES(status),
  sort_order = VALUES(sort_order);

INSERT INTO merchants (
  id, name, sales, min_order_price, delivery_fee, delivery_minutes,
  tags_json, description, notice, rating, logo_background, logo_text, sort_order
) VALUES
  ('m1', '沃尔玛精选超市', 12890, 0.00, 5.00, 28, '["满99减20","会员88折","极速退款"]', '品牌商超，覆盖生鲜百货与日常补给', '平台严选商家，支持预约配送与电子发票', 4.9, 'linear-gradient(180deg, #1f9af8 0%, #0e85f0 100%)', '沃尔', 10),
  ('m2', '永辉品质生活馆', 8620, 0.00, 0.00, 35, '["首单立减","0元配送","次日赔付"]', '社区高频消费精选，配送时效稳定', '晚间订单支持次晨优先送达', 4.8, 'linear-gradient(180deg, #00b894 0%, #00997a 100%)', '永辉', 20),
  ('m3', '山姆全球精选', 6510, 59.00, 12.00, 48, '["全球好物","企业专享","高端会员价"]', '大包装囤货好物，家庭与企业采购优选', '支持企业采购单与专属客服', 4.9, 'linear-gradient(180deg, #2d3436 0%, #636e72 100%)', '山姆', 30)
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  sales = VALUES(sales),
  min_order_price = VALUES(min_order_price),
  delivery_fee = VALUES(delivery_fee),
  delivery_minutes = VALUES(delivery_minutes),
  tags_json = VALUES(tags_json),
  description = VALUES(description),
  notice = VALUES(notice),
  rating = VALUES(rating),
  logo_background = VALUES(logo_background),
  logo_text = VALUES(logo_text),
  sort_order = VALUES(sort_order);

INSERT INTO product_spus (
  id, merchant_id, category_id, brand_id, name, subtitle, main_image,
  detail, detail_images_json, status, sort_order
) VALUES
  ('spu_p1', 'm1', 'fresh', 'brand_jd', '泰国进口金枕榴莲果肉 300g', '冷链到仓，新鲜果肉即开即食', '榴莲', '冷链到仓，新鲜果肉即开即食', '[]', 'ON_SHELF', 10),
  ('spu_p2', 'm1', 'fresh', 'brand_jd', '新疆阿克苏冰糖心苹果 1kg', '脆甜多汁，适合家庭囤货', '苹果', '脆甜多汁，适合家庭囤货', '[]', 'ON_SHELF', 20),
  ('spu_p3', 'm1', 'drink', 'brand_jd', '农夫山泉饮用天然水 550ml*12', '企业团购高频补货品', '矿泉水', '企业团购高频补货品', '[]', 'ON_SHELF', 30),
  ('spu_p4', 'm2', 'snack', 'brand_jd', '散装卫龙甜面筋 200g', '爆款休闲零食，办公室常备', '辣条', '爆款休闲零食，办公室常备', '[]', 'ON_SHELF', 10),
  ('spu_p5', 'm2', 'drink', 'brand_jd', '元气森林白桃气泡水 480ml*6', '低糖轻饮，活动福利专区', '气泡水', '低糖轻饮，活动福利专区', '[]', 'ON_SHELF', 20),
  ('spu_p6', 'm3', 'seafood', 'brand_jd', '挪威三文鱼切片 400g', '冷链直送，高蛋白轻食选择', '三文鱼', '冷链直送，高蛋白轻食选择', '[]', 'ON_SHELF', 10),
  ('spu_p7', 'm3', 'drink', 'brand_jd', '星巴克拿铁咖啡 270ml*10', '商务茶歇高频采购商品', '咖啡', '商务茶歇高频采购商品', '[]', 'ON_SHELF', 20)
ON DUPLICATE KEY UPDATE
  merchant_id = VALUES(merchant_id),
  category_id = VALUES(category_id),
  brand_id = VALUES(brand_id),
  name = VALUES(name),
  subtitle = VALUES(subtitle),
  main_image = VALUES(main_image),
  detail = VALUES(detail),
  detail_images_json = VALUES(detail_images_json),
  status = VALUES(status),
  sort_order = VALUES(sort_order);

INSERT INTO merchant_categories (id, merchant_id, category_id, name, sort_order) VALUES
  ('mc_m1_fresh', 'm1', 'fresh', '新鲜水果', 10),
  ('mc_m1_seafood', 'm1', 'seafood', '海鲜水产', 20),
  ('mc_m1_snack', 'm1', 'snack', '休闲零食', 30),
  ('mc_m1_drink', 'm1', 'drink', '酒水饮料', 40),
  ('mc_m2_fresh', 'm2', 'fresh', '新鲜水果', 10),
  ('mc_m2_snack', 'm2', 'snack', '休闲零食', 20),
  ('mc_m2_drink', 'm2', 'drink', '酒水饮料', 30),
  ('mc_m3_fresh', 'm3', 'fresh', '新鲜水果', 10),
  ('mc_m3_seafood', 'm3', 'seafood', '海鲜水产', 20),
  ('mc_m3_drink', 'm3', 'drink', '酒水饮料', 30)
ON DUPLICATE KEY UPDATE
  merchant_id = VALUES(merchant_id),
  category_id = VALUES(category_id),
  name = VALUES(name),
  sort_order = VALUES(sort_order);

INSERT INTO products (
  id, merchant_id, category_id, name, sales, price, original_price,
  image_text, unit, description, stock, sort_order
) VALUES
  ('p1', 'm1', 'fresh', '泰国进口金枕榴莲果肉 300g', 342, 39.90, 59.90, '榴莲', '份', '冷链到仓，新鲜果肉即开即食', 180, 10),
  ('p2', 'm1', 'fresh', '新疆阿克苏冰糖心苹果 1kg', 1205, 19.90, 29.90, '苹果', '袋', '脆甜多汁，适合家庭囤货', 320, 20),
  ('p3', 'm1', 'drink', '农夫山泉饮用天然水 550ml*12', 4500, 13.90, 15.90, '矿泉水', '箱', '企业团购高频补货品', 860, 30),
  ('p4', 'm2', 'snack', '散装卫龙甜面筋 200g', 500, 8.50, 10.00, '辣条', '袋', '爆款休闲零食，办公室常备', 540, 10),
  ('p5', 'm2', 'drink', '元气森林白桃气泡水 480ml*6', 880, 24.90, 29.90, '气泡水', '提', '低糖轻饮，活动福利专区', 240, 20),
  ('p6', 'm3', 'seafood', '挪威三文鱼切片 400g', 260, 69.90, 79.90, '三文鱼', '盒', '冷链直送，高蛋白轻食选择', 120, 10),
  ('p7', 'm3', 'drink', '星巴克拿铁咖啡 270ml*10', 660, 56.90, 65.90, '咖啡', '箱', '商务茶歇高频采购商品', 230, 20)
ON DUPLICATE KEY UPDATE
  merchant_id = VALUES(merchant_id),
  category_id = VALUES(category_id),
  name = VALUES(name),
  sales = VALUES(sales),
  price = VALUES(price),
  original_price = VALUES(original_price),
  image_text = VALUES(image_text),
  unit = VALUES(unit),
  description = VALUES(description),
  stock = VALUES(stock),
  sort_order = VALUES(sort_order);

INSERT INTO addresses (
  id, user_id, city, district, street, detail,
  contact_name, phone, tag, is_default
) VALUES (
  'addr_demo_1', 'u_demo', '北京市', '朝阳区', '大望路商务区', 'SOHO现代城 A 座 10 层 1008',
  '张采购', '13800000000', '公司', 1
) ON DUPLICATE KEY UPDATE
  city = VALUES(city),
  district = VALUES(district),
  street = VALUES(street),
  detail = VALUES(detail),
  contact_name = VALUES(contact_name),
  phone = VALUES(phone),
  tag = VALUES(tag),
  is_default = VALUES(is_default);
