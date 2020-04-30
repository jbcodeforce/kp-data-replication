package ibm.gse.eda.perf.consumer.dto;

import java.util.ArrayList;
import java.util.List;

public class CircularLinkedList<T> {
    private ListNode<T> head;
    private int numberOfElements = 0;
    private ListNode<T> tail;
    private int maxSize;

    public CircularLinkedList(int maxSize) {
        this.head = null;
        this.tail = null;
        this.maxSize = maxSize;
    }

    public T dataHead() {
        return head.data;
    }

    public void insertData(T data){
        ListNode<T> newNode = new ListNode<T>(data, null);
        if (head == null){
            head = newNode;
            tail = head;
            numberOfElements = 1;
        } else if (numberOfElements < maxSize ){
            tail.next = newNode;
            tail = tail.next;
            numberOfElements ++;
        }
        else if ( numberOfElements >=  maxSize){
            tail.next = newNode;
            tail = tail.next;
            head = head.next;
        }
       
    }

    public  List<T> getElements(){
        List<T> elements = new ArrayList<T>();
        ListNode<T> temp = head;
        while( temp != null ){
            elements.add((T)temp.data);
            temp = temp.next;
        }
        return elements;
    }
    @Override
    public String toString() {
        return getElements().toString();
    }

    /**
     * Append to this list the content of the given list
     * Keep the nb elements under maxSize
     * @param circular2
     */
	public void append(CircularLinkedList<T> circular2) {
        this.tail.next = circular2.head;
        numberOfElements =  numberOfElements + circular2.numberOfElements;
        if (numberOfElements > maxSize) {
            for( int i = 0; i < (numberOfElements - maxSize); i++) {
                this.head = head.next;
            }
            numberOfElements = maxSize;
        }
    }
    
    public long getNumberOfElements(){
        return numberOfElements;
    }
}

class ListNode<T> {
    public ListNode<T> next;
    public T data;

    public ListNode(T data, ListNode<T> next) {
        this.next = next;
        this.data = data;
    }

    public T data() {
        return data;
    }
}

