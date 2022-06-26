package ticketingsystem;

import java.util.*;

import java.util.concurrent.atomic.AtomicInteger;

class ThreadId {
    // Atomic integer containing the next thread ID to be assigned
    private static final AtomicInteger nextId = new AtomicInteger(0);

    // Thread local variable containing each thread's ID
    private static final ThreadLocal<Integer> threadId =
        new ThreadLocal<Integer>() {
            @Override protected Integer initialValue() {
                return nextId.getAndIncrement();
        }
    };

    // Returns the current thread's unique ID, assigning it if necessary
    public static int get() {
        return threadId.get();
    }
}

public class Test2 {
	static int threadnum = 4;
	final static int routenum = 10; // route is designed from 1 to 3
	final static int coachnum = 10; // coach is arranged from 1 to 5
	final static int seatnum = 100; // seat is allocated from 1 to 20
	final static int stationnum = 20; // station is designed from 1 to 5

	final static int testnum = 100000;
	final static int retpc = 10; // return ticket operation is 10% percent
	final static int buypc = 30; // buy ticket operation is 30% percent
	final static int inqpc = 100; //inquiry ticket operation is 60% percent
	
	static String passengerName() {
		Random rand = new Random();
		long uid = rand.nextInt(testnum);
		return "passenger" + uid; 
	}
	
	static int buy_count_total = 0;
	static int refund_count_total = 0;
	static int inquiry_count_total = 0;

	static long buy_time_total = 0;
	static long refund_time_total = 0;
	static long inquiry_time_total = 0;

	static boolean verbose = true; //是否要输出一些统计信息

	
	static synchronized void change(int bc,int rc,int ic,long bt,long rt,long it){
		buy_count_total += bc;
		refund_count_total += rc;
		inquiry_count_total += ic;
		
		buy_time_total += bt;
		refund_time_total += rt;
		inquiry_time_total += it;		
		
	}
	
	public static void main(String[] args) throws InterruptedException {
		
		threadnum = Integer.parseInt(args[0]);
		verbose = (Integer.parseInt(args[1]) == 1);
		
		Thread[] threads = new Thread[threadnum];
		
		final TicketingDS tds = new TicketingDS(routenum, coachnum, seatnum, stationnum, threadnum);		
		
		final Object mLock = new Object();
	    
		for (int i = 0; i< threadnum; i++) {
	    	threads[i] = new Thread(new Runnable() {
                public void run() {
            		Random rand = new Random();
                	Ticket ticket = new Ticket();
            		ArrayList<Ticket> soldTicket = new ArrayList<Ticket>();

					int buy_count = 0;
					int refund_count = 0;
					int inquiry_count = 0;

					long buy_time = 0;
					long refund_time = 0;
					long inquiry_time = 0;

            		
             		for (int i = 0; i < testnum; i++) {
            			int sel = rand.nextInt(inqpc);

            			if (0 <= sel && sel < retpc && soldTicket.size() > 0) { // return ticket
							int select = rand.nextInt(soldTicket.size());
							if ((ticket = soldTicket.remove(select)) != null) {
								long startTime = System.nanoTime();
								tds.refundTicket(ticket); //无所谓输出
								long duration = System.nanoTime() - startTime;
								refund_count += 1;
								refund_time += duration;
							} 
							else {
								continue;
							}
            			} 
						
						
						else if (retpc <= sel && sel < buypc) { // buy ticket
            				String passenger = passengerName();
            				int route = rand.nextInt(routenum) + 1;
            				int departure = rand.nextInt(stationnum - 1) + 1;
            				int arrival = departure + rand.nextInt(stationnum - departure) + 1; 
							
							long startTime = System.nanoTime();
							ticket = tds.buyTicket(passenger, route, departure, arrival);
							long duration = System.nanoTime() - startTime;

							buy_count += 1;
							buy_time += duration;
            				if (ticket != null) {
								soldTicket.add(ticket);
							}

            			} 			
						
						else if (buypc <= sel && sel < inqpc) { // inquiry ticket
            				
            				int route = rand.nextInt(routenum) + 1;
            				int departure = rand.nextInt(stationnum - 1) + 1;
            				int arrival = departure + rand.nextInt(stationnum - departure) + 1;

							long startTime = System.nanoTime();
            				int leftTicket = tds.inquiry(route, departure, arrival);
							long duration = System.nanoTime() - startTime;

							inquiry_count += 1;
							inquiry_time += duration;       			
            			}
            		}
					//统计信息					
					change(buy_count,refund_count,inquiry_count,buy_time,refund_time,inquiry_time);
					if(verbose){
						System.out.println("线程 " + ThreadId.get()+ "统计结束,调用次数:"+" "+refund_count+" "+buy_count+" "+inquiry_count+" 总计耗时:"+" "+refund_time+" "+buy_time+" "+inquiry_time);			
					}

                }
            });
			
			threads[i].start();
		}

	
	    for (int i = 0; i< threadnum; i++) {
	    	threads[i].join();
	    }
		
		double buy_time_aver = ((double) buy_time_total / buy_count_total) / 1;
		double refund_time_aver = ((double) refund_time_total / refund_count_total) / 1;
		double inquiry_time_aver = ((double) inquiry_time_total / inquiry_count_total) / 1;

		//用一个估计的运行时间来计算吞吐量，估计方式是所有线程的平均时间
		double average_time = ((double)(buy_time_total + refund_time_total + inquiry_time_total) / threadnum) / 1_000_000;
		double throughput = (buy_count_total + refund_count_total + inquiry_count_total) / average_time;

		if(verbose){			
			System.out.println("\n------------------------------------------------------");
			System.out.println("所有线程执行完毕");
	
			System.out.println("线程数 " + threadnum+" 调用次数"+" "+refund_count_total+" "+buy_count_total+" "+inquiry_count_total);
			System.out.println("线程数 " + threadnum+" 调用时间(ns)"+" "+refund_time_total+" "+buy_time_total+" "+inquiry_time_total);
			System.out.println("线程数 " + threadnum+" 平均用时(ns)"+" "+(int)refund_time_aver+" "+(int)buy_time_aver+" "+(int)inquiry_time_aver);
			System.out.println("线程数 " + threadnum+" 总吞吐量(per ms)" +" "+ String.format("%.2f", throughput));
			System.out.println("------------------------------------------------------\n");
			
		}
		else{
			System.out.println(threadnum +" "+ (int)refund_time_aver +" "+ (int)buy_time_aver +" "+ (int)inquiry_time_aver +" "+ String.format("%.2f", throughput));
			
		}
	}

}

