import { useMemberMessages, useSendCoachMessage } from '@/api/messagesApi'
import { MessageThread } from '@/components/messages/MessageThread'
import { useMemberDetail } from '@/pages/admin/MemberDetailPage'

export function MemberMessagesTab() {
  const { member } = useMemberDetail()
  const { data: messages, isLoading, error } = useMemberMessages(member.id)
  const sendMessage = useSendCoachMessage(member.id)

  return (
    <MessageThread
      messages={messages}
      viewpoint="COACH"
      isLoading={isLoading}
      error={error}
      onSend={(body) => sendMessage.mutate({ body })}
      isSending={sendMessage.isPending}
      sendError={sendMessage.isError ? sendMessage.error.message : null}
      emptyLabel="Bu üyeyle henüz yazışma yok."
    />
  )
}
