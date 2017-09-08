package cn.dq.orderfood.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import cn.dq.orderfood.common.Cons;
import cn.dq.orderfood.common.HttpResult;
import cn.dq.orderfood.common.KeyedLock;
import cn.dq.orderfood.common.KeyedLock.Lock;
import cn.dq.orderfood.common.exception.ApiException;
import cn.dq.orderfood.common.utils.DateUtils;
import cn.dq.orderfood.common.utils.MathUtils;
import cn.dq.orderfood.dao.AllottedMapper;
import cn.dq.orderfood.dao.MenuMapper;
import cn.dq.orderfood.dao.OrderInfoMapper;
import cn.dq.orderfood.dao.OrederMapper;
import cn.dq.orderfood.dao.RefundMapper;
import cn.dq.orderfood.dao.StoreTableMapper;
import cn.dq.orderfood.dao.SystemsMapper;
import cn.dq.orderfood.dao.employee.SalesMapper;
import cn.dq.orderfood.dao.member.GradeMapper;
import cn.dq.orderfood.dao.member.MemberCouponMapper;
import cn.dq.orderfood.dao.member.MemberMapper;
import cn.dq.orderfood.dao.promotion.CouponMapper;
import cn.dq.orderfood.dao.promotion.PromotionMapper;
import cn.dq.orderfood.dto.MenuDTO;
import cn.dq.orderfood.dto.MenuStockDTO;
import cn.dq.orderfood.enums.BooleanStatusEnum;
import cn.dq.orderfood.enums.GoodsStatusEnum;
import cn.dq.orderfood.enums.OrderStatusEnum;
import cn.dq.orderfood.enums.StoreTableStatusEnum;
import cn.dq.orderfood.model.Menu;
import cn.dq.orderfood.model.OrderInfo;
import cn.dq.orderfood.model.Oreder;
import cn.dq.orderfood.model.Refund;
import cn.dq.orderfood.model.StoreTable;
import cn.dq.orderfood.model.Systems;
import cn.dq.orderfood.model.employee.Sales;
import cn.dq.orderfood.model.member.Grade;
import cn.dq.orderfood.model.member.Member;
import cn.dq.orderfood.model.member.MemberCoupon;
import cn.dq.orderfood.model.promotion.Coupon;
import cn.dq.orderfood.model.promotion.Promotion;

/**
 * 成本价=菜单成本价 结算价=成本价*促销折扣 提成=结算价*提成率 利润=结算价-提成-成本价
 * 
 * 成本价=菜单成本价 结算价=会员价 提成=结算价*提成率 利润=结算价-提成-成本价
 * 
 * @author luoyh(Roy) - Aug 18, 2017
 */
@Service("orderService")
public class OrderService {

	@Autowired
	private OrederMapper orderMapper;
	@Autowired
	private OrderInfoMapper orderInfoMapper;
	@Autowired
	private StoreTableMapper storeTableMapper;
	@Autowired
	private MemberMapper memberMapper;
	@Autowired
	private GradeMapper gradeMapper;
	@Autowired
	private MenuMapper menuMapper;
	@Autowired
	private PromotionMapper promotionMapper;
	@Autowired
	private CouponMapper couponMapper;
	@Autowired
	private SalesMapper salesMapper;
	@Autowired
	private MemberCouponMapper memberCouponMapper;
	@Autowired
	private AllottedMapper allottedMapper;
	@Autowired
	private SystemsMapper systemsMapper;
	@Autowired
	private RefundMapper refundMapper;
	@Autowired
	private StoreService storeService;

	private static final Random RAND = new Random();
	
	/**
	 * 处理订单的入库业务, 此业务针对的是第一次入库.
	 * 比如: 此桌号没有订单, 或者此桌号没有确认后的订单, 否则会返回失败.
	 * @param memberId		会员id, 可为空
	 * @param storeId		门店id
	 * @param tableId		桌子id
	 * @param salesId		导购员id,可为空
	 * @param promotionId	促销活动id,可为空
	 * @param couponId		优惠券id, 可为空
	 * @param peopleNumber	就餐人数
	 * @param dtos			菜品信息, 里面最主要的是id和num两个熟悉
	 * @param straight		是否是直接为确认的, 一般在收银端的挂单入库, 此值为true
	 * @param foodStatus	出菜状态
	 * @return
	 * 
	 * @author luoyh(Roy) - Sep 1, 2017
	 */
	private HttpResult _execute(Long memberId, Long storeId, Long tableId, Long salesId, Long promotionId, Long couponId, Integer peopleNumber,
			List<MenuDTO> dtos, boolean straight, Integer foodStatus) {
		if (null == dtos || dtos.isEmpty()) {
			return HttpResult.err().msg("订单数据错误").build();
		}
		if (null != orderMapper.getByTableStatus(tableId, OrderStatusEnum.IN_USE.getValue())) {
			return HttpResult.err().msg("已有订单").build();
		}
		StoreTable table = storeTableMapper.get(tableId);
		if (null == table || table.getStoreId().longValue() != storeId.longValue()) {
			return HttpResult.err().msg("无效请求").build();
		}
		Sales sales = null == salesId ? null : salesMapper.selectById(salesId);
		if (null != sales && (sales.getStoreId().longValue() != storeId.longValue() || !"1".equals(sales.getSalesStatus()))) {
			return HttpResult.err().msg("无效导购员").build();
		}

		Member member = null == memberId ? null : memberMapper.selectById(memberId);
		if (null != member && !"1".equals(member.getMemberStatus())) {
			return HttpResult.err().msg("会员无效").build();
		}
		Grade grade = null == member ? null : gradeMapper.selectById(member.getGradeId());

		long now = System.currentTimeMillis();
		Promotion promotion = null == promotionId ? null : promotionMapper.selectById(promotionId);
		Coupon coupon = null == couponId ? null : couponMapper.selectById(couponId);
		if (!checkPromotion(promotion, now, storeId)) {
			return HttpResult.err().msg("促销活动无效").build();
		}
		if (!checkCoupon(coupon, storeId, now)) {
			return HttpResult.err().msg("优惠券无效").build();
		}
		if (null != promotion && null != coupon && !"1".equals(promotion.getPromotionCouponStatus())) {
			return HttpResult.err().msg("此促销活动不能使用优惠券").build();
		}
		if (null != coupon) {
			MemberCoupon mc = memberCouponMapper.getByCouponAndMember(couponId, memberId);
			if (null == mc) {
				return HttpResult.err().msg("优惠券无效").build();
			}
			if (nz(mc.getCouponNumber()) - nz(mc.getCouponUseNumber()) <= 0) {
				return HttpResult.err().msg("优惠券数量不足").build();
			}
		}

		List<Menu> menus = buildMenus(storeId, dtos);

		BigDecimal amount = Cons.ZERO; // 应付
		BigDecimal offAmount = Cons.ZERO; // 实付
		BigDecimal salesAmount = Cons.ZERO; // 提成
		BigDecimal profitAmount = Cons.ZERO; // 利润

		String code = generateOrderCode();
		// loop two
		// here loop two, because first calculate order amount,
		// seconds calculate activity.
		List<OrderInfo> infos = Lists.newArrayListWithCapacity(menus.size());
		List<MenuStockDTO> reduces = Lists.newArrayListWithCapacity(dtos.size());
		for (Menu menu : menus) {
			if (!"1".equals(menu.getMenuStatus())) {
				return HttpResult.err().msg(String.format("菜品[%s] 无效", menu.getGoodsInfoName())).build();
			}
			if ("1".equals(menu.getGoodsStatus())) {
				if (nz(menu.getGoodsNumber()) - menu.getExQunty() <= 0) {
					return HttpResult.err().msg(String.format("商品[%s]数量不足", menu.getGoodsInfoName())).build();
				}
				MenuStockDTO stock = new MenuStockDTO();
				stock.setMenuId(menu.getMenuId());
				stock.setAmount(menu.getExQunty());
				reduces.add(stock);
			}
			BigDecimal qunty = new BigDecimal(menu.getExQunty().toString());
			BigDecimal _amount = menu.getMenuPrice().multiply(qunty);
			BigDecimal _amountUnit = menu.getMenuPrice();
			amount = amount.add(_amount);
			if (null != member) {
				_amountUnit = menu.getMemberPrice();
				if ("1".equals(menu.getIsDiscount())) {
					_amountUnit = _amountUnit.multiply(grade.getGradeDiscount().divide(Cons.PERCENT));
				}
			}
			_amount = _amountUnit.multiply(qunty);
			offAmount = offAmount.add(_amount);
			menu.setExPrice(_amountUnit);
		}
		boolean canPromotion = null != promotion && offAmount.compareTo(promotion.getMoney()) > -1;
		offAmount = Cons.ZERO; // reset
		for (Menu menu : menus) {
			BigDecimal qunty = new BigDecimal(menu.getExQunty().toString());
			BigDecimal _amount = menu.getExPrice();
			BigDecimal _sale = Cons.ZERO;
			BigDecimal _profit = Cons.ZERO;
			if (canPromotion && canDiscountPromotion(promotion, member, menu.getMenuId())) {
				_amount = _amount.multiply(promotion.getPromotionDiscount().divide(Cons.PERCENT));
			}
			offAmount = offAmount.add(_amount.multiply(qunty));
			if (null != sales) {
				// 提成=折扣价*提成率
				_sale = _amount.multiply(menu.getMenuPercentage().divide(Cons.PERCENT));
				salesAmount = salesAmount.add(_sale.multiply(qunty));
			}
			// 利润=结算价-提成-成本价
			_profit = _amount.subtract(_sale).subtract(menu.getMenuCost());
			profitAmount = profitAmount.add(_profit.multiply(qunty));

			OrderInfo info = new OrderInfo();
			info.setMenuId(menu.getMenuId());
			info.setOrderNo(code);
			info.setOrderNumber(menu.getExQunty());
			info.setOrderPercentage(_sale);
			info.setOrderPrice(menu.getMenuCost());
			info.setOrderPrifit(_profit);
			info.setOrderStatement(_amount);
			info.setRefundNumber(0);
			info.setStoreTableId(tableId);
			infos.add(info);
		}
		
		if (canPromotion && !promotion.isDiscountPromotion()) {
			offAmount = offAmount.subtract(promotion.getPromotionDiscount());
		}

		if (null != coupon) {
			if (offAmount.compareTo(coupon.getUseInfo()) < 0) {
				return HttpResult.err().msg("消费金额不能使用优惠券").build();
			}
			offAmount = offAmount.subtract(coupon.getCouponMoney());
		}

		Systems systems = systemsMapper.getByStore(storeId);
		if (null != systems) {
			offAmount = offAmount.add(MathUtils.d(systems.getPeopleCash()).multiply(new BigDecimal(peopleNumber.toString())));
		}

		Oreder order = new Oreder();
		order.setOrederNo(code);
		order.setMemberId(memberId);
		order.setStoreId(storeId);
		order.setStoreTableId(tableId);
		order.setIsPromotion("1");
		order.setPayMoney(amount);
		order.setPayOffMoney(offAmount);
		order.setPayStatus(straight ? OrderStatusEnum.IN_USE.getValue() : OrderStatusEnum.UNSTART.getValue()); // 收银端挂单的订单直接为确认订单
		order.setOrderDate(new Date());
		order.setPeopleNumber(peopleNumber);
		order.setSalesId(salesId);
		order.setSalesMoney(salesAmount); // 提成
		order.setOrderProfit(profitAmount); // 利润
		order.setPromotionId(canPromotion ? promotionId : null);
		order.setCouponId(couponId);

		if (straight) { // pay_status=2
			if (!reduces.isEmpty()) {
				menuMapper.reduceBatch(reduces); // reduces menu stock
			}
			// 修改桌子为使用中
			table.setStoreTableStatus(StoreTableStatusEnum.BUSY.getValue());
			table.setFoodStatus(foodStatus); // set food_status
			storeTableMapper.update(table);

			// 取消预定状态
			allottedMapper.cancelReserve(tableId);

			if (null != coupon) {
				memberCouponMapper.decrement(memberId, couponId);
			}
			// delete other order when pay_staus=0
			deleteIllegals(tableId);
		}

		orderMapper.insert(order);
		orderInfoMapper.batchInserts(infos);

		return HttpResult.ok().data(code).build();
	}

	/**
	 * 这里只是增加了一个锁, 防止并发产生一个桌号出现多个已确认订单, 主要逻辑参照{@link #_execute(Long, Long, Long, Long, Long, Long, Integer, List, boolean, Integer)}
	 * @see #_execute(Long, Long, Long, Long, Long, Long, Integer, List, boolean, Integer)
	 * @param memberId
	 * @param storeId
	 * @param tableId
	 * @param salesId
	 * @param promotionId
	 * @param couponId
	 * @param peopleNumber
	 * @param dtos
	 * @param straight
	 * @param foodStatus
	 * @return
	 * 
	 * @author luoyh(Roy) - Sep 1, 2017
	 */
	@Transactional
	public HttpResult execute(Long memberId, Long storeId, Long tableId, Long salesId, Long promotionId, Long couponId, Integer peopleNumber,
			List<MenuDTO> dtos, boolean straight, Integer foodStatus) {
		if (null == tableId || null == storeId) {
			return HttpResult.err().msg("桌号无效").build();
		}
		if (storeService.isClosed(storeId)) {
			return HttpResult.err().msg("门店已打烊了").build();
		}
		if (straight) { // locked when straight is true
			try (Lock lock = KeyedLock.LOCK.acquire(tableId)) {
				return _execute(memberId, storeId, tableId, salesId, promotionId, couponId, peopleNumber, dtos, straight, foodStatus);
			} catch (Exception e) {
				throw new ApiException(Cons.ERR, "系统繁忙, 请重试");
			}
		}
		return _execute(memberId, storeId, tableId, salesId, promotionId, couponId, peopleNumber, dtos, straight, foodStatus);
	}

	/**
	 * 手机端的获取菜单接口, 如果有订单则返回以前的订单
	 * @param storeId
	 * @param tableId
	 * @param member
	 * @param dtos
	 * @return
	 * 
	 * @author luoyh(Roy) - Sep 1, 2017
	 */
	@Transactional
	public HttpResult menu(Long storeId, Long tableId, Member member, List<MenuDTO> dtos) {
		if (storeService.isClosed(storeId)) {
			return HttpResult.err().msg("门店已打烊了").build();
		}
		if (null == dtos || dtos.isEmpty()) {
			return HttpResult.err().msg("无效数据").build();
		}
		Oreder order = orderMapper.getByTableStatus(tableId, OrderStatusEnum.IN_USE.getValue());
		List<OrderInfo> oldInfos = null;
		if (null != order) {
			if (null == member || null == order.getMemberId() || order.getMemberId().longValue() != member.getMemberId().longValue()) {
				return HttpResult.err().msg("你不能点菜").build();
			}
		} else {
			if (null != member) {
				order = orderMapper.getByMember(member.getMemberId(), tableId, OrderStatusEnum.UNSTART.getValue());
			}
		}
		if (null != order) {
			oldInfos = orderInfoMapper.listByOrderNo(order.getOrederNo());
		}
		int initSize = null == oldInfos ? 0 : oldInfos.size();
		List<MenuDTO> olds = Lists.newArrayListWithCapacity(initSize);
		List<MenuDTO> news = Lists.newArrayListWithCapacity(dtos.size());
		List<Long> menuIds = Lists.newArrayListWithCapacity(initSize + dtos.size());
		if (null != oldInfos) {
			oldInfos.forEach(x -> menuIds.add(x.getMenuId()));
		}

		int total = 0;
		BigDecimal amount = Cons.ZERO;

		dtos.forEach(x -> menuIds.add(x.getId()));
		List<Menu> menus = menuMapper.listByIds(menuIds);
		if (null != oldInfos) {
			for (OrderInfo info : oldInfos) {
				for (Menu menu : menus) {
					if (info.getMenuId().longValue() == menu.getMenuId().longValue()) {
						MenuDTO dto = new MenuDTO();
						dto.setId(menu.getMenuId());
						dto.setIsDiscount(menu.getIsDiscount());
						dto.setName(menu.getGoodsInfoName());
						dto.setUrl(menu.getGoodsPic());
						dto.setStatus(menu.getMenuStatus());
						dto.setNum(MathUtils.subtract(info.getOrderNumber(), info.getRefundNumber()));
						dto.setPrice(menu.getMenuPrice());
						olds.add(dto);

						total += dto.getNum();
						amount = amount.add(menu.getMenuPrice().multiply(new BigDecimal(dto.getNum().toString())));
						break;
					}
				}
			}
		}
		
		for (MenuDTO e : dtos) {
			for (Menu m : menus) {
				if (null != e.getId() && e.getId().longValue() == m.getMenuId().longValue()) {
					MenuDTO dto = new MenuDTO();
					dto.setId(m.getMenuId());
					dto.setIsDiscount(m.getIsDiscount());
					dto.setName(m.getGoodsInfoName());
					dto.setUrl(m.getGoodsPic());
					dto.setStatus(m.getMenuStatus());
					dto.setPrice(m.getMenuPrice());
					dto.setNum(e.getNum());
					news.add(dto);
					
					total += dto.getNum();
					amount = amount.add(m.getMenuPrice().multiply(new BigDecimal(dto.getNum().toString())));
					break;
				}
			}
		}
		
		Map<String, Object> r = Maps.newHashMap();
		r.put("total", total);
		r.put("amount", amount);
		r.put("olds", olds);
		r.put("news", news);
		r.put("code", null == order ? "" : order.getOrederNo());
		
		return HttpResult.ok().data(r).build();
	}
	
	/**
	 * 手机端订单入库.
	 * 如果有以前的订单: 前订单状态为未确认, 则查出原订单信息, 然后删除原订单, 生成一个新订单.
	 * 前订单状态为已确认, 则增加原订单信息, 并重新计算价格.
	 * 原订单为已确认, 则不能修改优惠券和促销活动.
	 * @param code			前订单号
	 * @param memberId		会员id
	 * @param promotionId	促销活动id
	 * @param couponId		优惠券id
	 * @param tableId		桌号id
	 * @param storeId		门店id
	 * @param peopleNumber	就餐人数
	 * @param salesId		导购员id
	 * @param dtos			菜单信息
	 * @return
	 * 
	 * @author luoyh(Roy) - Sep 1, 2017
	 */
	@Transactional
	public HttpResult store(String code, 
							Long memberId, 
							Long promotionId,
							Long couponId, 
							Long tableId,
							Long storeId,
							Integer peopleNumber,
							Long salesId,
							List<MenuDTO> dtos) {
		if (storeService.isClosed(storeId)) {
			return HttpResult.err().msg("门店已打烊了").build();
		}
		if (StringUtils.isBlank(code)) { // not exists order, call execute
			return execute(memberId, storeId, tableId, salesId, promotionId, couponId, peopleNumber, dtos, false, 0);
		}
		if (null == dtos || dtos.isEmpty()) {
			return HttpResult.err().msg("菜单信息错误").build();
		}
		Oreder o = orderMapper.get(code);
		if (null == o) {
			return HttpResult.err().msg("无效订单").build();
		}
		Oreder order = orderMapper.getByTableStatus(tableId, OrderStatusEnum.IN_USE.getValue());
		if (null != order) {
			if (null == order.getMemberId() || !Objects.equals(order.getStoreTableId(), tableId) || order.getMemberId().longValue() != memberId.longValue()) {
				return HttpResult.err().msg("你不能点餐").build();
			}
		} else {
			order = orderMapper.getByMember(memberId, tableId, OrderStatusEnum.UNSTART.getValue());
		}
		if (null == order || !code.equals(order.getOrederNo())) {
			return HttpResult.err().msg("订单无效").build();
		}
		boolean fixed = OrderStatusEnum.IN_USE.getValue().equals(order.getPayStatus());
		if (fixed) {
			if (!Objects.equals(order.getCouponId(), couponId)) {
				return HttpResult.err().msg("订单已确认, 不能更改优惠券").build();
			}
			if (!Objects.equals(order.getPromotionId(), promotionId)) {
				return HttpResult.err().msg("订单已确认, 不能更改促销活动").build();
			}
		} else {
			orderInfoMapper.listByOrderNo(code).forEach(x -> {
				boolean find = false;
				for (MenuDTO d : dtos) {
					if (x.getMenuId().longValue() == d.getId().longValue()) {
						d.setNum(d.getNum() + x.getOrderNumber());
						find = true;
						break;
					}
				}
				if (!find) {
					MenuDTO e = new MenuDTO();
					e.setId(x.getMenuId());
					e.setNum(x.getOrderNumber());
					dtos.add(e);
				}
			});
			orderInfoMapper.deleteByOrderNo(code);
			orderMapper.deleteByOrderNo(code);
			promotionId = null == promotionId ? order.getPromotionId() : promotionId;
			couponId = null == couponId ? order.getCouponId() : couponId;
			HttpResult hr = execute(memberId, storeId, tableId, salesId, promotionId, couponId, peopleNumber, dtos, false, 0);
			if (Cons.OK != hr.getCode()) {
				throw new ApiException(hr);
			}
			return hr;
		}
		List<Menu> addMenus = buildMenus(storeId, dtos);
		if (null == addMenus || addMenus.isEmpty()) {
			return HttpResult.err().msg("菜单为空").build();
		}
		
		Promotion promotion = null == promotionId ? null : promotionMapper.selectById(promotionId);
		Sales sales = null == salesId ? null : salesMapper.selectById(salesId);
		Member member = memberMapper.selectById(memberId);
		Grade grade = gradeMapper.selectById(member.getGradeId());
		
		List<OrderInfo> increment = Lists.newArrayList();
		List<OrderInfo> addition = Lists.newArrayList();
		List<OrderInfo> infos = orderInfoMapper.listByOrderNo(code);
		List<MenuStockDTO> reduces = Lists.newArrayList();

		BigDecimal amount = Cons.ZERO; // 应付
		BigDecimal offAmount = Cons.ZERO; // 实付
		BigDecimal salesAmount = Cons.ZERO; // 提成
		BigDecimal profitAmount = Cons.ZERO; // 利润
		
		for (Menu menu : addMenus) {
			if (!"1".equals(menu.getMenuStatus())) {
				return HttpResult.err().msg(String.format("菜品[%s] 无效", menu.getGoodsInfoName())).build();
			}
			if ("1".equals(menu.getGoodsStatus())) {
				if (nz(menu.getGoodsNumber()) - menu.getExQunty() <= 0) {
					return HttpResult.err().msg(String.format("商品[%s]数量不足", menu.getGoodsInfoName())).build();
				}
				MenuStockDTO stock = new MenuStockDTO();
				stock.setMenuId(menu.getMenuId());
				stock.setAmount(menu.getExQunty());
				reduces.add(stock);
			}
			
			BigDecimal qunty = new BigDecimal(menu.getExQunty().toString());
			BigDecimal _amountUnit = menu.getMenuPrice();
			if (null != member) {
				if ("1".equals(menu.getIsDiscount())) {
					_amountUnit = menu.getMemberPrice().multiply(grade.getGradeDiscount().divide(Cons.PERCENT));
				} else {
					_amountUnit = menu.getMemberPrice();
				}
			}
			
			BigDecimal _sale = Cons.ZERO;
			BigDecimal _profit = Cons.ZERO;
			boolean ets = false;
			for (OrderInfo info : infos) {
				if (menu.getMenuId().longValue() == info.getMenuId().longValue()) {
					ets = true;
					
					OrderInfo in = new OrderInfo();
					in.setOrderInfoId(info.getOrderInfoId());
					in.setOrderNumber(info.getOrderNumber() + menu.getExQunty());
					_amountUnit = info.getOrderStatement();
					increment.add(in);
					
					_sale = info.getOrderPercentage();
					_profit = info.getOrderPrifit();
					break;
				}
			}
			if (!ets) {
				if (canDiscountPromotion(promotion, member, menu.getMenuId())) {
					_amountUnit = _amountUnit.multiply(promotion.getPromotionDiscount().divide(Cons.PERCENT));
				}
				if (null != sales) {
					// 提成=折扣价*提成率
					_sale = _amountUnit.multiply(menu.getMenuPercentage().divide(Cons.PERCENT));
				}
				// 利润=结算价-提成-成本价
				_profit = _amountUnit.subtract(_sale).subtract(menu.getMenuCost());

				OrderInfo info = new OrderInfo();
				info.setMenuId(menu.getMenuId());
				info.setOrderNo(code);
				info.setOrderNumber(menu.getExQunty());
				info.setOrderPercentage(_sale);
				info.setOrderPrice(menu.getMenuCost());
				info.setOrderPrifit(_profit);
				info.setOrderStatement(_amountUnit);
				info.setRefundNumber(0);
				info.setStoreTableId(tableId);
				addition.add(info);
			}
			
			salesAmount = salesAmount.add(_sale.multiply(qunty));
			profitAmount = profitAmount.add(_profit.multiply(qunty));
			
			BigDecimal _amount = _amountUnit.multiply(qunty);
			amount = amount.add(_amount);
			
			offAmount = offAmount.add(_amount);
		}
		
		if (!increment.isEmpty()) {
			orderInfoMapper.addOrder(increment);
		}
		if (!addition.isEmpty()) {
			orderInfoMapper.batchInserts(addition);
		}
		
		order.setPayMoney(order.getPayMoney().add(amount));
		order.setPayOffMoney(order.getPayOffMoney().add(offAmount));
		order.setSalesMoney(MathUtils.d(order.getSalesMoney()).add(salesAmount));
		order.setOrderProfit(MathUtils.d(order.getOrderProfit()).add(profitAmount));
		orderMapper.updateMoney(order);
		
		if (!reduces.isEmpty()) {
			menuMapper.reduceBatch(reduces);
		}
		
		return HttpResult.ok().data(code).build();
	}
	
	@Transactional(rollbackFor = Exception.class)
	public HttpResult confirm(Long storeId, String code) {
		if (storeService.isClosed(storeId)) {
			return HttpResult.err().msg("门店已打烊了").build();
		}
		//orderMapper.confirm(storeId, String code);
		Oreder order = orderMapper.get(code);
		if (null == order || order.getStoreId().longValue() != storeId.longValue()) {
			return HttpResult.err().msg("订单未找到").build();
		}
		if (!OrderStatusEnum.UNSTART.getValue().equals(order.getPayStatus())) {
			return HttpResult.err().msg("订单无效").build();
		}
		StoreTable table = storeTableMapper.get(order.getStoreTableId());
		if (null == table) {
			return HttpResult.err().msg("无效订单").build();
		}
		try (Lock lock = KeyedLock.LOCK.acquire(table.getStoreTableId())) {
			// 减库存
			List<MenuStockDTO> stocks = orderInfoMapper.loadOrderMenuStock(code);
			List<MenuStockDTO> reduces = Lists.newArrayListWithCapacity(stocks.size());
			for (MenuStockDTO stock : stocks) {
				if (GoodsStatusEnum.TYPE_GOODS.equals(stock.getType())) {
					if (stock.getRemain() - stock.getAmount() < 0) {
						return HttpResult.err().msg(String.format("%s 已经没有库存了", stock.getName())).build();
					}
					//if (stock.getLife().getTime() < TimeUnit.DAYS.toMillis(LocalDate.now().toEpochDay())) {
					//	return HttpResult.err().msg(String.format("%s 已经过期了", stock.getName())).build();
					//}
					reduces.add(stock);
				}
			}
			if (!reduces.isEmpty())	{
				menuMapper.reduceBatch(reduces);
			}
			
			// reduce coupon
			if (null != order.getCouponId() && null != order.getMemberId()) {
				memberCouponMapper.decrement(order.getMemberId(), order.getCouponId());
			}
			// reduce promotion
			if (null != order.getPromotionId()) {
				promotionMapper.decrement(order.getPromotionId());
			}

			Oreder newer = new Oreder();
			newer.setOrederNo(code);
			newer.setPayStatus(OrderStatusEnum.IN_USE.getValue());
			orderMapper.update(newer);
			
			table.setStoreTableStatus(StoreTableStatusEnum.BUSY.getValue());
			storeTableMapper.update(table);
			// 取消预定状态
			allottedMapper.cancelReserve(order.getStoreTableId());
			
			deleteIllegals(order.getStoreTableId());
			
			return HttpResult.ok().build();
		} catch (Exception e) {
			e.printStackTrace();
			throw new ApiException(Cons.ERR, "系统繁忙, 请重试");
		}
		
	}
	
	/**
	 * delete illegal status(0) order 
	 * @param tableId
	 * 
	 * @author luoyh(Roy) - Sep 1, 2017
	 */
	private void deleteIllegals(Long tableId) {
		//orderMapper.deleteIllegals(tableId);
		orderInfoMapper.deleteIllegal(tableId);
		orderMapper.deleteIllegal(tableId);
	}
	
	@Transactional
	public HttpResult returnConfirm(Long storeId, String code, String ids) {
		if (StringUtils.isBlank(code)) return HttpResult.err().msg("订单号不能为空").build();
		Oreder order = orderMapper.get(code);
		if (null == order) return HttpResult.err().msg("订单未找到").build();
		if (order.getStoreId().longValue() != storeId.longValue()) return HttpResult.err().msg("订单无效").build();
		if ("1".equals(order.getRefundStatus())) return HttpResult.err().msg("此订单不允许退菜").build();
		if (!OrderStatusEnum.IN_USE.getValue().equals(order.getPayStatus())) return HttpResult.err().msg("无效订单").build();
		if (StringUtils.isBlank(ids)) return HttpResult.err().msg("参数错误").build();
		
		Long[] refundIds = Stream.of(ids.split(",")).map(Long::valueOf).toArray(Long[]::new);
		List<Refund> refundData = refundMapper.listByIds(refundIds);
		if (null == refundData || refundData.isEmpty()) return HttpResult.err().msg("退菜信息为空").build();
		//if (refundData.stream().anyMatch(this::isConfirmRefund)) {
		//	return HttpResult.err().msg("有菜品已退了").build();
		//}
		
		List<MenuStockDTO> reduces = Lists.newArrayListWithCapacity(refundIds.length);

		List<OrderInfo> infos = orderInfoMapper.listByOrderNo(code);
		
		for (OrderInfo info : infos) {
			for (Refund refund : refundData) {
				if (isConfirmRefund(refund)) {
					return HttpResult.err().msg("有菜品已退了").build();
				}
				if (info.getOrderInfoId().longValue() == refund.getOrderInfoId().longValue()) {
					if (!hasMore(info, refund)) {
						return HttpResult.err().msg("有菜退菜数量不足").build();
					}
					
					MenuStockDTO ms = new MenuStockDTO();
					ms.setMenuId(info.getMenuId());
					ms.setAmount(refund.getRefundNumber());
					reduces.add(ms);

					info.setRefundNumber(MathUtils.nz(info.getRefundNumber()) + refund.getRefundNumber());
				}
			}
		}
		
		if (!reduces.isEmpty()) {
			menuMapper.restoreBatch(reduces);
		}
		handle(order, infos);
		refundMapper.confirm(refundIds);
		return HttpResult.ok().build();
	}
	
	public HttpResult refund(Long storeId, String code, BigDecimal amount) {
		if (StringUtils.isBlank(code)) {
			return HttpResult.err().msg("订单号错误").build();
		}
		if (null == amount || amount.compareTo(Cons.ZERO) <= 0) {
			return HttpResult.err().msg("退款金额错误").build();
		}
		Oreder order = orderMapper.get(code);
		if (null == order) {
			return HttpResult.err().msg("订单未找到").build();
		}
		if ("1".equals(order.getRefundStatus())) {
			return HttpResult.err().msg("此订单不允许退款").build();
		}
		if (!Objects.equals(order.getStoreId(), storeId)) {
			return HttpResult.err().msg("订单无效").build();
		}
		if (!OrderStatusEnum.PAID.getValue().equals(order.getPayStatus())) {
			return HttpResult.err().msg("订单无效").build();
		}
		if (order.getPayOffMoney().compareTo(amount.add(MathUtils.d(order.getRefundMoney()))) == -1) {
			return HttpResult.err().msg("退款金额不能大于支付金额").build();
		}
		orderMapper.refund(code, amount);
		return HttpResult.ok().build();
	}
	
	private boolean hasMore(OrderInfo info, Refund refund) {
		return info.getOrderNumber() - MathUtils.nz(info.getRefundNumber()) >= refund.getRefundNumber();
	}
	
	private boolean isConfirmRefund(Refund refund) {
		return "1".equals(refund.getRefundStatus());
	}
	
	private void notDiscountPromotion(Oreder order, Promotion promotion, Coupon coupon, Member member, Grade grade, List<Menu> menus, List<OrderInfo> infos, Sales sales) {
		BigDecimal amount = Cons.ZERO;		// 应付
		BigDecimal offAmount = Cons.ZERO;	// 实付
		BigDecimal salesAmount = Cons.ZERO;	// 提成
		BigDecimal profitAmount = Cons.ZERO;	// 利润
		boolean canCoupon = false;
		boolean canPromotion = false;
		for (Menu menu : menus) {
			for (OrderInfo info : infos) {
				if (info.getMenuId().longValue() == menu.getMenuId().longValue()) {
					menu.setExQunty(MathUtils.subtract(info.getOrderNumber(), info.getRefundNumber()));
					BigDecimal qunty = new BigDecimal(menu.getExQunty().toString());

					BigDecimal _amount = menu.getMenuPrice().multiply(qunty);
					BigDecimal _amountUnit = menu.getMenuPrice();
					amount = amount.add(_amount);
					
					if (null != member) {
						if ("1".equals(menu.getIsDiscount())) {
							_amountUnit = menu.getMemberPrice().multiply(grade.getGradeDiscount().divide(Cons.PERCENT));
						} else {
							_amountUnit = menu.getMemberPrice();
						}
					}
					_amount = _amountUnit.multiply(qunty);
					offAmount = offAmount.add(_amount);
					menu.setExPrice(_amountUnit);
					
					BigDecimal _sale = Cons.ZERO;
					BigDecimal _profit = Cons.ZERO;
					if (null != sales) {
						// 提成=折扣价*提成率
						_sale = _amountUnit.multiply(menu.getMenuPercentage().divide(Cons.PERCENT));
						salesAmount = salesAmount.add(_sale.multiply(qunty));
					}
					// 利润=结算价-提成-成本价
					_profit = _amountUnit.subtract(_sale).subtract(menu.getMenuCost());
					profitAmount = profitAmount.add(_profit.multiply(qunty));
					
					info.setOrderPercentage(_sale);
					info.setOrderPrice(menu.getMenuCost());
					info.setOrderPrifit(_profit);
					info.setOrderStatement(_amount);
					break;
				}
			}
		}
		
		if (null != promotion && !promotion.isDiscountPromotion() && offAmount.compareTo(promotion.getMoney()) > -1) {
			offAmount = offAmount.subtract(promotion.getPromotionDiscount());
			canPromotion = true;
		}
		
		if ((null == promotion || "1".equals(promotion.getPromotionCouponStatus())) && null != coupon && offAmount.compareTo(coupon.getUseInfo()) > -1) {
			offAmount = offAmount.subtract(coupon.getCouponMoney());
			canCoupon = true;
		}

		Systems systems = systemsMapper.getByStore(order.getStoreId());
		if (null != systems) {
			offAmount = offAmount.add(MathUtils.d(systems.getPeopleCash()).multiply(new BigDecimal(order.getPeopleNumber().toString())));
		}
		
		order.setPayMoney(amount);
		order.setPayOffMoney(offAmount);
		order.setSalesMoney(salesAmount); // 提成
		order.setOrderProfit(profitAmount); // 利润
		orderMapper.updateMoney(order);
		
		orderMapper.updateActivity(order.getOrederNo(), canPromotion ? order.getPromotionId() : null, canCoupon ? order.getCouponId() : null);
		restoreActivity(order.getMemberId(), canPromotion ? null : order.getPromotionId(), canCoupon ? null : order.getCouponId());
		orderInfoMapper.batchUpdate(infos);
	}
	
	private void unknownPromotion(Oreder order, Promotion promotion, Coupon coupon, Member member, Grade grade, List<Menu> menus, List<OrderInfo> infos, Sales sales) {
		BigDecimal amount = Cons.ZERO;		// 应付
		BigDecimal offAmount = Cons.ZERO;	// 实付
		BigDecimal salesAmount = Cons.ZERO;	// 提成
		BigDecimal profitAmount = Cons.ZERO;	// 利润
		
		for (Menu menu : menus) {
			for (OrderInfo info : infos) {
				if (info.getMenuId().longValue() == menu.getMenuId().longValue()) {
					menu.setExQunty(MathUtils.subtract(info.getOrderNumber(), info.getRefundNumber()));
					BigDecimal qunty = new BigDecimal(MathUtils.subtract(info.getOrderNumber(), info.getRefundNumber()).toString());

					BigDecimal _amount = menu.getMenuPrice().multiply(qunty);
					BigDecimal _amountUnit = menu.getMenuPrice();
					amount = amount.add(_amount);
					
					if (null != member) {
						if ("1".equals(menu.getIsDiscount())) {
							_amountUnit = menu.getMemberPrice().multiply(grade.getGradeDiscount().divide(Cons.PERCENT));
						} else {
							_amountUnit = menu.getMemberPrice();
						}
					}
					_amount = _amountUnit.multiply(qunty);
					offAmount = offAmount.add(_amount);
					menu.setExPrice(_amountUnit);
					break;
				}
			}
		}
		
		boolean canCoupon = false;
		boolean canPromotion = null != promotion && offAmount.compareTo(promotion.getMoney()) > -1;
		offAmount = Cons.ZERO; // reset
		for (Menu menu : menus) {
			for (OrderInfo info : infos) {
				if (info.getMenuId().longValue() == menu.getMenuId().longValue()) {
					menu.setExQunty(MathUtils.subtract(info.getOrderNumber(), info.getRefundNumber()));
					BigDecimal qunty = new BigDecimal(menu.getExQunty().toString());

					BigDecimal _amount = menu.getExPrice(); // single price
					BigDecimal _sale = Cons.ZERO;
					BigDecimal _profit = Cons.ZERO;
					if (canPromotion && canDiscountPromotion(promotion, member, menu.getMenuId())) {
						_amount = _amount.multiply(promotion.getPromotionDiscount().divide(Cons.PERCENT));
					}
					offAmount = offAmount.add(_amount.multiply(qunty));
					if (null != sales) {
						// 提成=折扣价*提成率
						_sale = _amount.multiply(menu.getMenuPercentage().divide(Cons.PERCENT));
						salesAmount = salesAmount.add(_sale.multiply(qunty));
					}
					// 利润=结算价-提成-成本价
					_profit = _amount.subtract(_sale).subtract(menu.getMenuCost());
					profitAmount = profitAmount.add(_profit.multiply(qunty));
					
					info.setOrderPercentage(_sale);
					info.setOrderPrice(menu.getMenuCost());
					info.setOrderPrifit(_profit);
					info.setOrderStatement(_amount);
					break;
				}
			}
		}
		
		if ((null == promotion || "1".equals(promotion.getPromotionCouponStatus())) && null != coupon && offAmount.compareTo(coupon.getUseInfo()) > -1) {
			offAmount = offAmount.subtract(coupon.getCouponMoney());
			canCoupon = true;
		}

		Systems systems = systemsMapper.getByStore(order.getStoreId());
		if (null != systems) {
			offAmount = offAmount.add(MathUtils.d(systems.getPeopleCash()).multiply(new BigDecimal(order.getPeopleNumber().toString())));
		}
		
		order.setPayMoney(amount);
		order.setPayOffMoney(offAmount);
		order.setSalesMoney(salesAmount); // 提成
		order.setOrderProfit(profitAmount); // 利润
		orderMapper.updateMoney(order);
		
		orderMapper.updateActivity(order.getOrederNo(), canPromotion ? order.getPromotionId() : null, canCoupon ? order.getCouponId() : null);
		restoreActivity(order.getMemberId(), canPromotion ? null : order.getPromotionId(), canCoupon ? null : order.getCouponId());
		orderInfoMapper.batchUpdate(infos);
	}
	
	private void handle(Oreder order, List<OrderInfo> infos) {
		Promotion promotion = null == order.getPromotionId() ? null : promotionMapper.selectById(order.getPromotionId());
		Coupon coupon = null == order.getCouponId() ? null : couponMapper.selectById(order.getCouponId());
		Sales sales = null == order.getSalesId() ? null : salesMapper.selectById(order.getSalesId());
		Member member = null == order.getMemberId() ? null : memberMapper.selectById(order.getMemberId());
		Grade grade = null == member ? null : gradeMapper.selectById(member.getGradeId());

		List<Long> menuIds = Lists.newArrayListWithCapacity(infos.size());
		infos.forEach(x -> menuIds.add(x.getMenuId()));
		
		List<Menu> menus = menuMapper.listByIdsInStore(menuIds, order.getStoreId());
		
		if (null == promotion || !promotion.isDiscountPromotion()) {
			notDiscountPromotion(order, promotion, coupon, member, grade, menus, infos, sales);
		} else {
			unknownPromotion(order, promotion, coupon, member, grade, menus, infos, sales);
		}
		
	}
	
	private void restoreActivity(Long memberId, Long promotionId, Long couponId) {
		if (null != memberId) {
			if (null != promotionId) {
				promotionMapper.restorePromotion(promotionId);
			}
			if (null != couponId) {
				memberCouponMapper.restoreCoupon(memberId, couponId);
			}
		}
	}
	

	private boolean canDiscountPromotion(Promotion promotion, Member member, Long menuId) {
		if (null == promotion) {
			return false;
		}
		if (!promotion.isDiscountPromotion()) {
			return false;
		}
		if ((null != member && "1".equals(promotion.getPromotionMember())) || (null == member && "0".equals(promotion.getPromotionMember()))) {
			if ("0".equals(promotion.getIsDiscount())) {
				return true;
			}
			if ("1".equals(promotion.getIsDiscount()) && isIn(menuId, promotion.getDiscountGoods())) {
				return true;
			}
		}
		return false;
	}

	private Integer nz(Integer v) {
		return MathUtils.nz(v);
	}

	private Long nz(Long v) {
		return MathUtils.nz(v);
	}

	private List<Menu> buildMenus(Long storeId, List<MenuDTO> dtos) {
		return toMenus(storeId, dtos).stream().filter(x -> {
			for (MenuDTO m : dtos) {
				if (m.getNum() <= 0) {
					continue;
				}
				if (m.getId().longValue() == x.getMenuId().longValue()) {
					x.setExQunty(m.getNum());
					return true;
				}
			}
			return false;
		}).collect(Collectors.toList());
	}

	private boolean checkPromotion(Promotion promotion, long now, Long storeId) {
		if (null == promotion) {
			return true;
		}
		if (promotion.getStoreId().longValue() != storeId.longValue()) {
			return false;
		}
		if (BooleanStatusEnum.DISABLED.equals(promotion.getPromotionStatus())) {
			return false;
		}
		if ("1".equals(promotion.getIsLimitNumber()) && promotion.getPromotionNumber().intValue() <= MathUtils.nz(promotion.getUseNumber()).intValue()) {
			return false;
		}
		if (promotion.getStartDate().getTime() > now || promotion.getEndDate().getTime() <= now) {
			return false;
		}
		return true;
	}

	private boolean checkCoupon(Coupon coupon, Long storeId, long now) {
		if (null == coupon)
			return true;
		if (coupon.getStoreId().longValue() != storeId.longValue()) {
			return false;
		}
		if (Integer.parseInt(DateUtils.epochMillisToTimeString(coupon.getCouponValidDay().getTime(), "yyyyMMdd")) < Integer.parseInt(DateUtils.epochMillisToTimeString(now, "yyyyMMdd"))) {
			return false;
		}
		if (coupon.getCouponStartTime().getTime() > now) {
			return false;
		}
		if (!"1".equals(coupon.getCouponStatus())) {
			return false;
		}
		return true;
	}

	private List<Menu> toMenus(Long storeId, List<MenuDTO> dtos) {
		return menuMapper.listByIdsInStore(dtos.stream().map(e -> e.getId()).collect(Collectors.toList()), storeId);
	}

	private String generateOrderCode() {
		return DateTimeFormatter.ofPattern("yyMMddHHmmssSSS").format(LocalDateTime.now()) + StringUtils.leftPad(RAND.nextInt(100000) + "", 5, '0');
	}

	private boolean isIn(Long menuId, String ids) {
		return !StringUtils.isBlank(ids) && Stream.of(ids.split(",")).map(Long::valueOf).anyMatch(id -> id.longValue() == menuId.longValue());
	}

}
