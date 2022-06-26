package ticketingsystem;

import java.util.Arrays;
import java.lang.reflect.Array;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.ConcurrentHashMap;

public class TicketingDS implements TicketingSystem {
	//ToDo
	private static AtomicLong[][][][] tid1; //车票编号
	private int routenum; //列车车次数目
	private int coachnum; //车厢数目
	private int seatnum; //座位数目
	private int stationnum; //站数目
	private int threadnum; //线程数目
	private int threadrange1=256;//线程阈值
	private Seat[] routemap;//车次图，用于存储每个车次的座位占用情况
	private int seatsum; //单趟列车总座位数目，因为每节车厢的座位数目没有意义
	private ConcurrentHashMap<Long, Ticket> soldTicket;//存储已购买车票
	private long timetag=0L;//时间戳

	public TicketingDS(){ //无参数初始化为缺省值
		routenum=5;
		coachnum=8;
		seatnum=100;
		stationnum=10;
		threadnum=16;
		seatsum=coachnum*seatnum;
		if(threadnum>threadrange1){//根据线程数不同采用不同的车次图方式
			routemap=new Seat_morethread[routenum];
			for (int i = 0; i < routenum; i++) {
            	routemap[i] = new Seat_morethread(seatsum);
       		}
		}
		else{
			routemap=new Seat_lessthread[routenum];
			for (int i = 0; i < routenum; i++) {
				routemap[i] = new Seat_lessthread(seatsum);
			}
			tid1=new AtomicLong[routenum][stationnum-1][stationnum][seatsum];
			for(int i=0;i<routenum;i++){
				for(int j=0;j<stationnum-1;j++){
					for(int m=0;m<stationnum;m++){
						for(int n=0;n<seatsum;n++){
							tid1[i][j][m][n]=new AtomicLong();
						}
					}
				}
			}
		}
		soldTicket=new ConcurrentHashMap<Long, Ticket>();
	}

	public TicketingDS(int routenum, int coachnum, int seatnum, int stationnum, int threadnum) { 
		this.routenum=routenum;
		this.coachnum=coachnum;
		this.seatnum=seatnum;
		this.stationnum=stationnum;
		this.threadnum=threadnum;
		seatsum=coachnum*seatnum;
		if(threadnum>threadrange1){//根据线程数不同采用不同的车次图方式
			routemap=new Seat_morethread[routenum];
			for (int i = 0; i < routenum; i++) {
            	routemap[i] = new Seat_morethread(seatsum);
       		}
		}
		else{
			routemap=new Seat_lessthread[routenum];
			for (int i = 0; i < routenum; i++) {
				routemap[i] = new Seat_lessthread(seatsum);
			}
			tid1=new AtomicLong[routenum][stationnum-1][stationnum][seatsum];
			for(int i=0;i<routenum;i++){
				for(int j=0;j<stationnum-1;j++){
					for(int m=0;m<stationnum;m++){
						for(int n=0;n<seatsum;n++){
							tid1[i][j][m][n]=new AtomicLong();
						}
					}
				}
			}
		}
		soldTicket=new ConcurrentHashMap<Long, Ticket>();
	}

	//退票方法满足可线性化要求 
	//对有效的Ticket对象返回true，对错误或无效的Ticket对象返回false
	//退票后，原车票的tid作废
	@Override
	public boolean refundTicket(Ticket ticket) {
		Ticket temp=soldTicket.get(ticket.tid);
		if(temp==null||temp.route!=ticket.route||temp.arrival!=ticket.arrival||temp.departure!=ticket.departure||temp.coach!=ticket.coach){
			return false;
		}
		int i= (ticket.coach-1)*seatnum+ticket.seat-1;
		if(threadnum>threadrange1){//只要能够退票就修改当前时间戳防止重复座位编号
			timetag=SystemClock.millisClock().now();
		}
		else{
			tid1[ticket.route-1][ticket.departure-1][ticket.arrival-1][i].getAndIncrement();
		}
        if(routemap[ticket.route - 1].refundTicket(ticket,i)){
			soldTicket.remove(ticket.tid);
			return true;
		}
        else{
			return false;
		}
	}

	//tid编号
	//针对每个座位都有特定的编号
	public long tidcoder(int route, int departure, int arrival, int i) {
		long coder;
		if(threadnum>threadrange1){//获取系统时间较长
			coder=(timetag<<25)+((long)i<<14)+((long)route<<10)+((long)departure<<5)+((long)arrival);
		}
		else{
			coder=(tid1[route-1][departure-1][arrival-1][i].get()<<25)+((long)i<<14)+((long)route<<10)+((long)departure<<5)+((long)arrival);
		}
		return coder;
	}
	//买票方法满足可线性化要求
	//若购票成功，返回有效的Ticket对象；若失败（即无余票），返回无效的Ticket对象（即return null）
	//离开站已经确定大于出发站
	//每个区段有余票时，系统必须满足该区段的购票请求。
	//车票不能超卖，系统不能卖无座车票。
	//每一个tid的车票只能出售一次。
	@Override
	public Ticket buyTicket(String passenger, int route, int departure, int arrival) {
		if (route<=0||route>this.routenum||arrival>this.stationnum||arrival<=0||departure>=arrival||departure<=0||departure>=this.stationnum){
			return null;
		}
		int i = routemap[route - 1].buyTicket(departure, arrival);
        if (i >= 0){
			Ticket ticket = new Ticket();
			ticket.tid=tidcoder(route, departure, arrival, i);
			ticket.passenger=passenger;
			ticket.route=route;
			ticket.coach=i/seatnum+1;
			ticket.seat=i%seatnum+1;
			ticket.departure=departure;
			ticket.arrival=arrival;
			soldTicket.put(ticket.tid,ticket);
            return  ticket;
        }
		else{
			return null;
		}
	}

	//查询余票方法满足静态一致性
	//查询route车次从departure站到arrival站的余票数
	//离开站已经确定大于出发站
	@Override
	public int inquiry(int route, int departure, int arrival){
		if (route<=0||route>this.routenum||arrival>this.stationnum||arrival<=0||departure>=arrival||departure<=0||departure>=this.stationnum){
			return -1;
		}
		int num = routemap[route - 1].inquery(departure, arrival);
		return num;
	}
}