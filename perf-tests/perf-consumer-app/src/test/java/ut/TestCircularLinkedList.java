package ut;

import org.junit.Test;

import ibm.gse.eda.perf.consumer.dto.CircularLinkedList;
import org.junit.Assert;

public class TestCircularLinkedList {

    @Test
    public void shouldHaveMatchingNumberOfElement(){
        CircularLinkedList<String> circular = new CircularLinkedList<String>(5);
        for( int i = 0; i < 2; i++){
            circular.insertData(String.format("%s is my data.", i));
        }
        Assert.assertTrue(circular.getElements().size() == 2);
        System.out.println(circular.dataHead());
        Assert.assertTrue(circular.dataHead().equals("0 is my data."));
        System.out.println(circular.toString());
    }

    @Test
    public void shouldHaveMatchingNumberOfIntegerElement(){
        CircularLinkedList<Integer> circular2 = new CircularLinkedList<Integer>(5);
        for( int i = 0; i < 20; i++){
            circular2.insertData(i);
        }
        Assert.assertTrue(circular2.getNumberOfElements() == 5);
        System.out.println(circular2.toString());
    }

    @Test
    public void shouldAppendList() {
        CircularLinkedList<Integer> circular1 = new CircularLinkedList<Integer>(5);
        for( int i = 0; i < 5; i++){
            circular1.insertData(i);
        }
        CircularLinkedList<Integer> circular2 = new CircularLinkedList<Integer>(10);
        for( int i = 5; i < 15; i++){
            circular2.insertData(i);
        }
        circular1.append(circular2);
        Assert.assertTrue(circular1.getNumberOfElements() == 5);
        Assert.assertTrue(circular1.dataHead().intValue() == 10);

    }
}