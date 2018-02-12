package org.clarksnut.batchs.messages.mail;

import org.clarksnut.mail.*;
import org.clarksnut.models.jpa.entity.FileEntity;
import org.clarksnut.models.jpa.entity.BrokerEntity;
import org.clarksnut.models.jpa.entity.MessageEntity;
import org.clarksnut.models.utils.XmlValidator;

import javax.batch.api.chunk.ItemProcessor;
import javax.inject.Inject;
import javax.inject.Named;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Named
public class PullMailMessagesProcessor implements ItemProcessor {

    @Inject
    private MailUtils mailUtils;

    @Inject
    private XmlValidator xmlValidator;

    @Override
    public Object processItem(Object item) throws Exception {
        BrokerEntity brokerEntity = (BrokerEntity) item;


        Map<BrokerEntity, List<MessageEntity>> result = new HashMap<>();
        result.put(brokerEntity, new ArrayList<>());


        MailProvider mailProvider = mailUtils.getMailReader(brokerEntity.getType());
        if (mailProvider != null) {
            MailRepositoryModel repository = MailRepositoryModel.builder()
                    .email(brokerEntity.getEmail())
                    .refreshToken(brokerEntity.getUser().getOfflineToken())
                    .build();

            MailQuery.Builder queryBuilder = MailQuery.builder().fileType("xml");
            Date lastTimeSynchronized = brokerEntity.getLastTimeSynchronized();
            if (lastTimeSynchronized != null) {
                queryBuilder.after(lastTimeSynchronized.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
            }

            TreeSet<MailUblMessageModel> messages = mailProvider.getUblMessages(repository, queryBuilder.build());
            for (MailUblMessageModel message : messages) {
                MessageEntity messageEntity = new MessageEntity();
                messageEntity.setId(UUID.randomUUID().toString());
                messageEntity.setBroker(brokerEntity);
                messageEntity.setMessageId(message.getMessageId());
                messageEntity.setInternalDate(message.getReceiveDate());

                for (MailAttachment attachment : message.getXmlFiles()) {
                    byte[] bytes = attachment.getBytes();
                    if (xmlValidator.isValidUblFile(bytes)) {
                        FileEntity fileEntity = new FileEntity();
                        fileEntity.setId(UUID.randomUUID().toString());
                        fileEntity.setFilename(attachment.getFilename());
                        fileEntity.setFile(bytes);
                        fileEntity.setMessage(messageEntity);

                        messageEntity.getFiles().add(fileEntity);
                    }
                }

                result.get(brokerEntity).add(messageEntity);
            }

            // Update last sync
            MailUblMessageModel lastMessage = messages.last();
            brokerEntity.setLastTimeSynchronized(lastMessage.getReceiveDate());
        }

        return result;
    }

}