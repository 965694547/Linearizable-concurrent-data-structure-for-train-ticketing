package ticketingsystem;

import java.util.*;

public class Valid {
	
	final static int threadnum = 16; //线程总数 缺省为16
	
	final static int routenum = 10; // route is designed from 1 to 3 车次总数，缺省为5
	final static int coachnum = 10; // coach is arranged from 1 to 5 列车车厢数目，缺省为8
	final static int seatnum = 100; // seat is allocated from 1 to 20 每节车厢的座位数，缺省为8
	final static int stationnum = 20; // station is designed from 1 to 5 每个车次停站的数量，缺省为10

	final static int testnum = 100000; //测试总次数 用于输出乘客名称
	final static int retpc = 10; // return ticket operation is 10% percent 退票10%
	final static int buypc = 30; // buy ticket operation is 30% percent 购票30%
	final static int inqpc = 100; //inquiry ticket operation is 60% percent 查询余票60% 由100向下生成随机数

	static long time = 0L;
	static long buyTicketTime = 0L;
	static long refundTicketTime = 0L;
	static long inquiryTicketTime = 0L;

	static long buyTotal = 0L;
	static long refundTotal = 0L;
	static long inquiryTotal = 0L;

	static String passengerName(){ //根据测试总数输出乘客名称
		Random rand = new Random();
		long uid = rand.nextInt(testnum);
		return "passenger" + uid; 
	}

	public static void main(String[] args) throws InterruptedException {
		//ToDo
		final int threadtemp = 64;
		final int threadtemplength = 30;
	    final long startTime = System.nanoTime();//获取系统时间
		int temp;
		for(temp=0;temp<threadtemplength;temp++){
			Thread[] threads = new Thread[threadtemp];
			final TicketingDS tds = new TicketingDS(routenum, coachnum, seatnum, stationnum, threadtemp); //多线程售票系统
			for (int i = 0; i< threadtemp; i++){
				threads[i] = new Thread(new Runnable(){ //new Thread(Runnable target).start()启动线程
					public void run(){
						Random rand = new Random();//为每个乘客生成随机数
						Ticket ticket = new Ticket();
						ArrayList<Ticket> soldTicket = new ArrayList<Ticket>();//已经售出的票
						
						for (int i = 0; i < testnum; i++){
							int sel = rand.nextInt(inqpc); //rand.nextInt随机返回大于0小于100的数确定乘客行为
							if (0 <= sel && sel < retpc && soldTicket.size() > 0){ // return ticket
									int select = rand.nextInt(soldTicket.size());//随机选择一张票退票
									ticket = soldTicket.remove(select);
									long preTime = System.nanoTime() - startTime;
									tds.refundTicket(ticket);
									long postTime = System.nanoTime() - startTime;
									refundTicketTime+=(postTime-preTime);
									refundTotal++;
							}
							else if (retpc <= sel && sel < buypc){ // buy ticket
								String passenger = passengerName();
								int route = rand.nextInt(routenum);//随机选择车次购票
								int departure = rand.nextInt(stationnum - 1);//随机选择离开站
								int arrival = rand.nextInt(stationnum);
								long preTime = System.nanoTime() - startTime;
								ticket = tds.buyTicket(passenger, route, departure, arrival);//购票成功
								long postTime = System.nanoTime() - startTime;
								buyTicketTime+=(postTime-preTime);
								buyTotal++;
								if (ticket != null) {
									soldTicket.add(ticket);
								}
							} 
							else if (buypc <= sel && sel < inqpc){ // inquiry ticket
								int route = rand.nextInt(routenum);//随机选择车次查询
								int departure = rand.nextInt(stationnum - 1);//随机选择离开站查询
								int arrival = rand.nextInt(stationnum); // arrival is always greater than departure
								long preTime = System.nanoTime() - startTime;
								int leftTicket = tds.inquiry(route, departure, arrival);
								long postTime = System.nanoTime() - startTime;
								inquiryTicketTime +=(postTime-preTime);
								inquiryTotal++;
							}
						}
					}
				});
			}
			long start = System.currentTimeMillis();
			for (int i = 0; i < threadtemp; ++i){
				threads[i].start();//执行当前线程操作
			}
			for (int i = 0; i< threadtemp; i++){
				threads[i].join();//每个线程以字符串形式输出
			}
			long end = System.currentTimeMillis();
			time += end - start;
			long buyAvgTime = (long) (buyTicketTime / buyTotal);
			long refundAvgTime = (long) (refundTicketTime /refundTotal);
			long inquiryAvgTime = (long) (inquiryTicketTime /inquiryTotal);
			long Throughput = (long) (threadtemp*testnum*(temp-threadtemplength/2+1)/ (double)time);
			if(temp==0){
				System.out.println(String.format("预热阶段"));
			}
			else if(temp==threadtemplength/2){
				System.out.println(String.format("测试阶段"));
			}
			if(temp<threadtemplength/2){
				Throughput = (long) (threadtemp*testnum/ (double)time);
				time=0L;
				buyTicketTime = 0L;
				refundTicketTime = 0L;
				inquiryTicketTime = 0L;
			
				buyTotal = 0L;
				refundTotal = 0L;
				inquiryTotal = 0L;	
			}
			System.out.println(String.format(
				"ThreadNum: %d BuyAvgTime(纳秒): %d RefundAvgTime(纳秒): %d InquiryAvgTime(纳秒): %d ThroughOut(操作数/毫秒): %d",
				threadtemp, buyAvgTime, refundAvgTime, inquiryAvgTime, Throughput));
		}
	}
}
