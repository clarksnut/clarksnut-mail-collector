package org.clarksnut.batchs.messages.step1;

import org.clarksnut.models.jpa.entity.BrokerEntity;
import org.jberet.support.io.JpaItemWriter;

import javax.inject.Named;
import java.util.List;

@Named
public class B_DisableBrokersWithInvalidRefreshToken extends JpaItemWriter {

    @Override
    public void writeItems(final List<Object> items) throws Exception {
        if (entityTransaction) {
            em.getTransaction().begin();
        }

        for (final Object e : items) {
            BrokerEntity brokerEntity = (BrokerEntity) e;
            em.merge(brokerEntity);
        }

        if (entityTransaction) {
            em.getTransaction().commit();
        }
    }

}
