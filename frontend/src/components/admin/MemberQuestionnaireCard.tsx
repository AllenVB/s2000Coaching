import { useMemberQuestionnaire } from '@/api/questionnaireApi'
import { formatDate } from '@/lib/date'
import { EQUIPMENT, GOALS } from '@/types/questionnaire'

/** The coach's view of the starting questionnaire, with any "yes" answers pulled out and spelled out. */
export function MemberQuestionnaireCard({ memberId }: { memberId: string }) {
  const { data } = useMemberQuestionnaire(memberId)

  if (!data) {
    return null
  }

  if (!data.answers) {
    return (
      <section className="rounded-2xl border border-border bg-surface p-6">
        <h2 className="mb-2 text-sm font-bold uppercase tracking-wider text-text-primary">Sağlık Anketi</h2>
        <p className="text-sm italic text-text-muted">Üye başlangıç anketini henüz doldurmadı.</p>
      </section>
    )
  }

  const { answers } = data
  const flagged = data.questions.filter((_question, index) => answers.parq[index])

  return (
    <section
      className={`rounded-2xl border p-6 ${
        answers.needsMedicalClearance ? 'border-error/40 bg-error/5' : 'border-border bg-surface'
      }`}
    >
      <h2 className="mb-3 text-sm font-bold uppercase tracking-wider text-text-primary">Sağlık Anketi</h2>
      <p className="mb-3 text-xs text-text-muted">{formatDate(answers.completedAt)} tarihinde dolduruldu.</p>

      {answers.needsMedicalClearance ? (
        <div className="mb-4 rounded-xl border border-error/40 bg-surface p-3.5">
          <p className="mb-2 text-xs font-semibold text-error">
            Doktor onayı önerilir — "evet" yanıtlanan sorular:
          </p>
          <ul className="list-disc space-y-1 pl-5 text-xs leading-relaxed text-text-primary">
            {flagged.map((question) => (
              <li key={question}>{question}</li>
            ))}
          </ul>
        </div>
      ) : (
        <p className="mb-4 text-xs text-success">Tüm sağlık sorularına "hayır" yanıtı verildi.</p>
      )}

      <dl className="grid gap-3 text-sm sm:grid-cols-3">
        <Item label="Hedef" value={GOALS.find((g) => g.value === answers.goal)?.label ?? answers.goal} />
        <Item label="Haftada" value={`${answers.trainingDaysPerWeek} gün`} />
        <Item label="Ortam" value={EQUIPMENT.find((e) => e.value === answers.equipment)?.label ?? answers.equipment} />
      </dl>
      {answers.injuries ? (
        <p className="mt-3 whitespace-pre-line text-xs leading-relaxed text-text-secondary">
          <strong className="text-text-primary">Not: </strong>
          {answers.injuries}
        </p>
      ) : null}
    </section>
  )
}

function Item({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <dt className="text-[11px] font-semibold uppercase tracking-wider text-text-muted">{label}</dt>
      <dd className="font-semibold text-text-primary">{value}</dd>
    </div>
  )
}
