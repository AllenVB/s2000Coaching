import { Link } from 'react-router-dom'

import {
  useCreateMessageTemplate,
  useDeleteMessageTemplate,
  useMemberMessages,
  useMessageTemplates,
  useSendCoachMessage,
} from '@/api/messagesApi'
import { MessageThread } from '@/components/messages/MessageThread'
import { useMemberDetail } from '@/pages/admin/MemberDetailPage'

export function MemberMessagesTab() {
  const { member } = useMemberDetail()
  const { data: messages, isLoading, error } = useMemberMessages(member.id)
  const sendMessage = useSendCoachMessage(member.id)
  const { data: templates } = useMessageTemplates()
  const createTemplate = useCreateMessageTemplate()
  const deleteTemplate = useDeleteMessageTemplate()

  return (
    <div className="space-y-4">
      <p className="text-xs text-text-muted">
        Tüm yazışmaları tek listede görmek için{' '}
        <Link
          to={`/yonetim/mesajlar?uye=${member.id}`}
          className="font-semibold text-primary hover:underline"
        >
          mesaj merkezini
        </Link>{' '}
        kullanabilirsiniz.
      </p>
      <MessageThread
        messages={messages}
        viewpoint="COACH"
        isLoading={isLoading}
        error={error}
        onSend={(body) => sendMessage.mutate({ body })}
        isSending={sendMessage.isPending}
        sendError={sendMessage.isError ? sendMessage.error.message : null}
        emptyLabel="Bu üyeyle henüz yazışma yok."
        quickReplies={templates ?? []}
        onSaveQuickReply={(quickReply) => createTemplate.mutate(quickReply)}
        onDeleteQuickReply={(id) => deleteTemplate.mutate({ templateId: id })}
      />
    </div>
  )
}
