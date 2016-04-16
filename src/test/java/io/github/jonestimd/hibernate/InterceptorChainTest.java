package io.github.jonestimd.hibernate;

import org.hibernate.Interceptor;
import org.hibernate.Transaction;
import org.hibernate.type.Type;
import org.junit.Test;
import org.mockito.InOrder;

import static org.mockito.Mockito.*;

public class InterceptorChainTest {
    private Interceptor interceptor1 = mock(Interceptor.class);
    private Interceptor interceptor2 = mock(Interceptor.class);
    private InterceptorChain interceptorChain = new InterceptorChain(interceptor1, interceptor2);

    @Test
    public void onFlushDirtyCallsInterceptorsInOrder() throws Exception {
        Object entity = new Object();
        long id = -1L;
        Object[] currentState = new Object[] {};
        Object[] previousState = new Object[] {};
        String[] propertyNames = new String[] {};
        Type[] types = new Type[] {};

        interceptorChain.onFlushDirty(entity, id, currentState, previousState, propertyNames, types);

        InOrder inOrder = inOrder(interceptor1, interceptor2);
        inOrder.verify(interceptor1).onFlushDirty(same(entity), same(id), same(currentState), same(previousState),
                same(propertyNames), same(types));
        inOrder.verify(interceptor2).onFlushDirty(same(entity), same(id), same(currentState), same(previousState),
                same(propertyNames), same(types));
    }

    @Test
    public void onSaveCallsInterceptorsInOrder() throws Exception {
        Object entity = new Object();
        long id = -1L;
        Object[] state = new Object[] {};
        String[] propertyNames = new String[] {};
        Type[] types = new Type[] {};

        interceptorChain.onSave(entity, id, state, propertyNames, types);

        InOrder inOrder = inOrder(interceptor1, interceptor2);
        inOrder.verify(interceptor1).onSave(same(entity), same(id), same(state), same(propertyNames), same(types));
        inOrder.verify(interceptor2).onSave(same(entity), same(id), same(state), same(propertyNames), same(types));
    }

    @Test
    public void onDeleteCallsInterceptorsInOrder() throws Exception {
        Object entity = new Object();
        long id = -1L;
        Object[] state = new Object[] {};
        String[] propertyNames = new String[] {};
        Type[] types = new Type[] {};

        interceptorChain.onDelete(entity, id, state, propertyNames, types);

        InOrder inOrder = inOrder(interceptor1, interceptor2);
        inOrder.verify(interceptor1).onDelete(same(entity), same(id), same(state), same(propertyNames), same(types));
        inOrder.verify(interceptor2).onDelete(same(entity), same(id), same(state), same(propertyNames), same(types));
    }

    @Test
    public void afterTransactionBeginCallsInterceptorsInOrder() throws Exception {
        Transaction tx = mock(Transaction.class);
        
        interceptorChain.afterTransactionBegin(tx);
        
        InOrder inOrder = inOrder(interceptor1, interceptor2);
        inOrder.verify(interceptor1).afterTransactionBegin(tx);
        inOrder.verify(interceptor2).afterTransactionBegin(tx);
        verifyZeroInteractions(tx);
    }
    
    @Test
    public void afterTransactionCompletionCallsInterceptorsInOrder() throws Exception {
        Transaction tx = mock(Transaction.class);
        
        interceptorChain.afterTransactionCompletion(tx);
        
        InOrder inOrder = inOrder(interceptor1, interceptor2);
        inOrder.verify(interceptor1).afterTransactionCompletion(tx);
        inOrder.verify(interceptor2).afterTransactionCompletion(tx);
        verifyZeroInteractions(tx);
    }
}
