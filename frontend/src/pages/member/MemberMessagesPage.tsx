import { useMyMessages, useSendMyMessage } from '@/api/messagesApi'
import { MessageThread } from '@/components/messages/MessageThread'
import { PageHeading } from '@/components/ui/PageHeading'

export function MemberMessagesPage() {
  const { data: messages, isLoading, error } = useMyMessages()
  const sendMessage = useSendMyMessage()

  return (
    <>
      <PageHeading
        title="Mesajlar"
        description="Koçunuzla yazışma. Haftalık geri bildiriminizi buradan iletebilirsiniz."
      />
      <MessageThread
        messages={messages}
        viewpoint="MEMBER"
        isLoading={isLoading}
        error={error}
        onSend={(body) => sendMessage.mutate({ body })}
        isSending={sendMessage.isPending}
        sendError={sendMessage.isError ? sendMessage.error.message : null}
        emptyLabel="Henüz mesaj yok. İlk mesajı siz yazabilirsiniz."
      />
    </>
  )
}
