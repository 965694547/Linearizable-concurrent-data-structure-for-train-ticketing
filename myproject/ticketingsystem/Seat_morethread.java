package ticketingsystem;

import ticketingsystem.Seat;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class Seat_morethread implements Seat {
    private int seatsum; //单趟列车总座位数目，因为每节车厢的座位数目没有意义

    private AtomicLong[] seatmap; //座位图，用于存储每个座位在每一站被占用的情况
    private ConcurrentHashMap<Long, AtomicInteger> priorefund; //操作记录，key为出发站和终止站编码，value值为两种情况，当整个车次承载量<单趟列车总座位数目时，value为最小退票位置；当当整个车次承载量>=单趟列车总座位数目时,value为最小退票位置或购票的下一个位置
    private LongAdder capicity; //当前整个车次承载量
    private int cap; //当前整个车次承载量，LongAdder在较多线程可能会出错且速度较慢

    public Seat_morethread(int seatsum){
		this.seatsum=seatsum;
        seatmap=new AtomicLong[seatsum];
        for (int i=0; i<seatsum; i++) {
            seatmap[i] = new AtomicLong();
        }
        priorefund = new ConcurrentHashMap<Long, AtomicInteger>();
        capicity = new LongAdder();
    }

    private long encoder(int departure, int arrival){//输出出发站和终止站编码
        long coder=0;
        for (int i=departure-1;i<arrival-1;i++){
            coder += 1 << i;
        }
        return coder;
    }
    
    public int buyTicket(int departure, int arrival){//购买车票
        long coder = encoder(departure,arrival);
        if (!priorefund.containsKey(coder)) { //如果没有买过该票，创建该票
            priorefund.put(coder, new AtomicInteger());
        }
        long newseat, oldseat;
        int end = seatsum;
        if(cap<seatsum){
            end =cap;
            for (int i = cap; i < seatsum; i++){
                AtomicLong currmap=seatmap[i];
                oldseat=currmap.get();//座位占用情况
                while ((oldseat & coder) == 0){//座位未被占用
                    newseat = oldseat | coder;
                    if (currmap.compareAndSet(oldseat, newseat)){//执行成功说明购票成功
                        capicity.increment();
                        return i;
                    }
                    oldseat=currmap.get();
                }
            }
            cap=capicity.intValue();
        }
        AtomicInteger currpriorefund = priorefund.get(coder);//退回车票
        int currnum = currpriorefund.get();
        for (int i = currnum; i <end; i++){
            AtomicLong currmap=seatmap[i];
            oldseat=currmap.get();//座位占用情况
            while ((oldseat & coder) == 0){//座位未被占用
                newseat = oldseat | coder;
                if (currmap.compareAndSet(oldseat, newseat)){//执行成功说明购票成功
                    currpriorefund.compareAndSet(currnum,i+1);//当前位置被购买只能期待下一个位置没有被购买
                    return i;
                }
                oldseat=currmap.get();
            }
        }
        for (int i = 0; i <currnum; i++){
            AtomicLong currmap=seatmap[i];
            oldseat=currmap.get();//座位占用情况
            while ((oldseat & coder) == 0){//座位未被占用
                newseat = oldseat | coder;
                if (currmap.compareAndSet(oldseat, newseat)){//执行成功说明购票成功
                    currpriorefund.compareAndSet(currnum,i+1);//当前位置被购买只能期待下一个位置没有被购买
                    return i;
                }
                oldseat=currmap.get();
            }
        }
        return -1;
    }

    public boolean refundTicket(Ticket ticket, int i){//退回车票
        AtomicLong currmap = seatmap[i];
        long oldseat = currmap.get();//座位占用情况
        long newseat;
        long coder = encoder(ticket.departure, ticket.arrival);
        AtomicInteger currpriorefund = priorefund.get(coder);
        while ((oldseat & coder) == coder){//座位被占用
            newseat = (~coder) &oldseat;//如果座位未被占用
            if (currmap.compareAndSet(oldseat,newseat)) {//执行成功说明退票成功
                int currnum = currpriorefund.get();
                while (i<currnum) {//循环执行直到更小的座位被退
                    currpriorefund.compareAndSet(currnum, i);
                    currnum = currpriorefund.get();
                }
                return true;
            }
            oldseat = currmap.get();
        }
        return false;
    }

    public int inquery(int departure, int arrival) {//查询车票
        int num = 0;//余票数
        long coder = encoder(departure, arrival);
        for (int i = 0; i < seatsum; i++) {
            AtomicLong currcoder = seatmap[i];
            if ((currcoder.get() & coder) == 0)//座位未被占用
                num++;
        }
        return num;
    }
}