import { dayTypeLabel, splitBlocks, type Meal, type NutritionDayType, type NutritionPlan } from '@/types/nutrition'

/**
 * The plan as the member reads it: one table per block, food on the left and the amount the
 * coach set on the right — the same shape as the coach's own printed plans. An amount the
 * coach has not written yet shows as a dash rather than an empty cell, so it reads as
 * "not set" and not as "none".
 */
export function NutritionTable({ plan, dayType }: { plan: NutritionPlan; dayType: NutritionDayType }) {
  const { meals, supplements } = splitBlocks(plan, dayType)
  const day = dayTypeLabel(dayType).toLowerCase()

  return (
    <div className="grid gap-6 lg:grid-cols-[3fr_2fr] lg:items-start">
      <BlockTable title="Beslenme Programı" blocks={meals} emptyText={`Bu plana ${day} için henüz öğün eklenmedi.`} />
      <div className="space-y-6">
        <BlockTable title="Supplement Planı" blocks={supplements} emptyText={`Bu planda ${day} için supplement yok.`} />
        {plan.waterLiters !== null ? (
          <section className="rounded-2xl border border-border bg-surface p-5">
            <h2 className="text-sm font-bold uppercase tracking-wider text-text-primary">Günlük Su</h2>
            <p className="mt-2 font-display text-2xl font-bold text-text-primary">
              {formatLiters(plan.waterLiters)}
              <span className="ml-1 text-xs font-medium text-text-muted">litre</span>
            </p>
          </section>
        ) : null}
      </div>
    </div>
  )
}

function BlockTable({ title, blocks, emptyText }: { title: string; blocks: Meal[]; emptyText: string }) {
  return (
    <section className="overflow-hidden rounded-2xl border border-border bg-surface">
      <h2 className="border-b border-border px-5 py-4 text-sm font-bold uppercase tracking-wider text-text-primary">
        {title}
      </h2>
      {blocks.length === 0 ? (
        <p className="p-5 text-sm italic text-text-muted">{emptyText}</p>
      ) : (
        <table className="w-full border-collapse text-sm">
          <thead className="sr-only">
            <tr>
              <th scope="col">Besin</th>
              <th scope="col">Miktar</th>
            </tr>
          </thead>
          {blocks.map((block) => (
            <tbody key={block.id} className="border-b border-border last:border-b-0">
              <tr className="bg-surface-elevated">
                <th
                  colSpan={2}
                  scope="colgroup"
                  className="px-5 py-2.5 text-left text-xs font-bold uppercase tracking-wider text-primary"
                >
                  {block.name}
                  {block.timeHint ? (
                    <span className="ml-2 font-normal normal-case tracking-normal text-text-muted">
                      {block.timeHint}
                    </span>
                  ) : null}
                </th>
              </tr>
              {block.note ? (
                <tr>
                  <td colSpan={2} className="px-5 pt-2 text-xs leading-relaxed text-text-secondary">
                    {block.note}
                  </td>
                </tr>
              ) : null}
              {block.items.length === 0 ? (
                <tr>
                  <td colSpan={2} className="px-5 py-3 text-xs italic text-text-muted">
                    Henüz kalem yok.
                  </td>
                </tr>
              ) : (
                block.items.map((item) => (
                  <tr key={item.id} className="border-t border-border/60 first:border-t-0">
                    <td className="px-5 py-2.5 align-top text-text-primary">
                      {item.name}
                      {item.note ? (
                        <span className="block text-xs text-text-secondary">{item.note}</span>
                      ) : null}
                    </td>
                    <td className="w-2/5 px-5 py-2.5 text-right align-top font-semibold text-text-primary">
                      {item.amount ? item.amount : <span className="font-normal text-text-muted">—</span>}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          ))}
        </table>
      )}
    </section>
  )
}

/** 4.0 reads as "4"; 2.5 stays "2,5" — Turkish decimal comma. */
function formatLiters(liters: number): string {
  return Number.isInteger(liters) ? String(liters) : String(liters).replace('.', ',')
}
