package ibm.gse.eda.perf.consumer.kafka;

import ibm.gse.eda.perf.consumer.app.Message;

import java.util.ArrayList;
import java.util.List;

public class CircularLinkedList<T> {
    private ListNode head;
    private int numberOfElements = 0;
    private ListNode tail;
    private int maxSize;

    public CircularLinkedList(int maxSize) {
        this.head = null;
        this.tail = null;
        this.maxSize = maxSize;
    }

    public void insertData(T data){
        ListNode newNode = new ListNode(data, null);
        if (head == null){
            head = newNode;
            tail = head;
        } else if (numberOfElements < maxSize ){
            tail.next = newNode;
            tail = tail.next;
        }
        else if ( numberOfElements >=  maxSize){
            tail.next = newNode;
            tail = tail.next;
            head = head.next;
        }
        numberOfElements ++;
    }

    @Override
    public String toString() {
        List<T> listofMessage = new ArrayList<>();
        ListNode temp = head;
        while( temp != null ){
            listofMessage.add((T)temp.data);
            temp = temp.next;
        }
        return listofMessage.toString();
    }
}

class ListNode<T> {
    public ListNode next;
    public T data;

    public ListNode(T data, ListNode next) {
        this.next = next;
        this.data = data;
    }
}

class Test{
    public static void main(String[] args) {

        CircularLinkedList<String> circular = new CircularLinkedList(5);
        for( int i = 0; i < 2; i++){
            circular.insertData(String.format("%s is my data.", i));
        }
        System.out.println(circular.toString());
        CircularLinkedList<Integer> circular2 = new CircularLinkedList(5);
        for( int i = 0; i < 20; i++){
            circular2.insertData(i);
        }
        System.out.println(circular2.toString());

    }
}