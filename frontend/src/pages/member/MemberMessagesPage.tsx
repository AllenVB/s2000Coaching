import { useSearchParams } from 'react-router-dom'

import { useMyMessages, useSendMyMessage } from '@/api/messagesApi'
import { MessageThread } from '@/components/messages/MessageThread'
import { PageHeading } from '@/components/ui/PageHeading'

export function MemberMessagesPage() {
  const [searchParams] = useSearchParams()
  // The "Koçuna sor" shortcuts on the home screen arrive with their question in the URL.
  const draft = searchParams.get('soru') ?? undefined

  const { data: messages, isLoading, error } = useMyMessages()
  const sendMessage = useSendMyMessage()

  return (
    <>
      <PageHeading
        title="Mesajlar"
        description="Koçunuzla yazışma. Haftalık geri bildiriminizi buradan iletebilirsiniz."
      />
      <MessageThread
        // A new question from the home screen should replace the untouched draft.
        key={draft ?? ''}
        messages={messages}
        viewpoint="MEMBER"
        isLoading={isLoading}
        error={error}
        onSend={(body) => sendMessage.mutate({ body })}
        isSending={sendMessage.isPending}
        sendError={sendMessage.isError ? sendMessage.error.message : null}
        emptyLabel="Henüz mesaj yok. İlk mesajı siz yazabilirsiniz."
        initialDraft={draft}
      />
    </>
  )
}
