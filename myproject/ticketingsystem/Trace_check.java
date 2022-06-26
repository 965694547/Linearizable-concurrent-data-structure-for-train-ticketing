package ticketingsystem;

import java.util.*;

import java.util.concurrent.atomic.AtomicInteger;

class ThreadId {
    // Atomic integer containing the next thread ID to be assigned
    private static final AtomicInteger nextId = new AtomicInteger(0);

    // Thread local variable containing each thread's ID
    private static final ThreadLocal<Integer> threadId =
        new ThreadLocal<Integer>() {
            @Override protected Integer initialValue() { //@Override 可以当注释用，可以验证方法名是否是父类所有
                return nextId.getAndIncrement();
        }
    };

    // Returns the current thread's unique ID, assigning it if necessary
    public static int get() {
        return threadId.get();
    }
}

public class Trace_check {
	final static int threadnum = 5; //线程总数 缺省为16
	final static int routenum = 1; // route is designed from 1 to 3 车次总数，缺省为5
	final static int coachnum = 3; // coach is arranged from 1 to 5 列车车厢数目，缺省为8
	final static int seatnum = 5; // seat is allocated from 1 to 20 每节车厢的座位数，缺省为8
	final static int stationnum = 5; // station is designed from 1 to 5 每个车次停站的数量，缺省为10

	final static int testnum = 500; //测试总次数 用于输出乘客名称
	final static int retpc = 30; // return ticket operation is 10% percent 退票10%
	final static int buypc = 60; // buy ticket operation is 30% percent 购票30%
	final static int inqpc = 100; //inquiry ticket operation is 60% percent 查询余票60% 由100向下生成随机数
	
	static String passengerName() { //根据测试总数输出乘客名称
		Random rand = new Random();
		long uid = rand.nextInt(testnum);
		return "passenger" + uid; 
	}

	public static void main(String[] args) throws InterruptedException { //InterruptedException线程被中断时的异常
        
		 
		Thread[] threads = new Thread[threadnum];
		
		final TicketingDS tds = new TicketingDS(routenum, coachnum, seatnum, stationnum, threadnum); //多线程售票系统
		
		final long startTime = System.nanoTime();//获取系统时间
	    
	for (int i = 0; i< threadnum; i++) {
	    	threads[i] = new Thread(new Runnable() { //new Thread(Runnable target).start()启动线程
                public void run() {
            		Random rand = new Random();//为每个乘客生成随机数
                	Ticket ticket = new Ticket();
            		ArrayList<Ticket> soldTicket = new ArrayList<Ticket>();//已经售出的票
            		
             		for (int i = 0; i < testnum; i++) {
            			int sel = rand.nextInt(inqpc); //rand.nextInt随机返回大于0小于100的数确定乘客行为
            			if (0 <= sel && sel < retpc && soldTicket.size() > 0) { // return ticket
            				int select = rand.nextInt(soldTicket.size());//随机选择一张票退票
           				if ((ticket = soldTicket.remove(select)) != null) {//如果所退的票不为空，即可以退票
											long preTime = System.nanoTime() - startTime;
            					if (tds.refundTicket(ticket)) {
												long postTime = System.nanoTime() - startTime;
            						System.out.println(preTime + " " + postTime + " " + ThreadId.get() + " " + "TicketRefund" + " " + ticket.tid + " " + ticket.passenger + " " + ticket.route + " " + ticket.coach  + " " + ticket.departure + " " + ticket.arrival + " " + ticket.seat);
            						System.out.flush();//清空缓存
            					} else {
            						System.out.println(preTime + " " + String.valueOf(System.nanoTime()-startTime) + " " + ThreadId.get() + " " + "ErrOfRefund");
            						System.out.flush();
            					}
            				} else {//如果所退的票为空，不可以退票
											long preTime = System.nanoTime() - startTime;
            					System.out.println(preTime + " " + String.valueOf(System.nanoTime()-startTime) + " " + ThreadId.get() + " " + "ErrOfRefund");
        						System.out.flush();
            				}
            			} else if (retpc <= sel && sel < buypc) { // buy ticket
            				String passenger = passengerName();
            				int route = rand.nextInt(routenum) + 1;//随机选择车次购票
            				int departure = rand.nextInt(stationnum - 1) + 1;//随机选择离开站
            				int arrival = departure + rand.nextInt(stationnum - departure) + 1; // arrival is always greater than departure
										long preTime = System.nanoTime() - startTime;
            				if ((ticket = tds.buyTicket(passenger, route, departure, arrival)) != null) {//购票成功
											long postTime = System.nanoTime() - startTime;
            					System.out.println(preTime + " " + postTime + " " + ThreadId.get() + " " + "TicketBought" + " " + ticket.tid + " " + ticket.passenger + " " + ticket.route + " " + ticket.coach + " " + ticket.departure + " " + ticket.arrival + " " + ticket.seat);
            					soldTicket.add(ticket);//给已卖出的票增加一张
        						System.out.flush();
            				} else {//购票失败
            					System.out.println(preTime + " " + String.valueOf(System.nanoTime()-startTime) + " " + ThreadId.get() + " " + "TicketSoldOut" + " " + route + " " + departure+ " " + arrival);
        						System.out.flush();
            				}
            			} else if (buypc <= sel && sel < inqpc) { // inquiry ticket
            				
            				int route = rand.nextInt(routenum) + 1;//随机选择车次查询
            				int departure = rand.nextInt(stationnum - 1) + 1;//随机选择离开站查询
            				int arrival = departure + rand.nextInt(stationnum - departure) + 1; // arrival is always greater than departure
										long preTime = System.nanoTime() - startTime;
            				int leftTicket = tds.inquiry(route, departure, arrival);
										long postTime = System.nanoTime() - startTime;	         			
            			}
            		}

                }
            });
              threads[i].start();//执行当前线程操作
 	    }
	
	    for (int i = 0; i< threadnum; i++) {
	    	threads[i].join();//每个线程以字符串形式输出
	    }		
	}
}