import { useState } from 'react'

import { SectionHeading } from '@/components/ui/SectionHeading'
import { ChevronDownIcon } from '@/components/ui/icons'
import { faqs } from '@/data/content'

export function Faq() {
  const [openIndex, setOpenIndex] = useState<number | null>(0)

  return (
    <section id="sss" className="border-b border-border bg-surface/40 py-20 sm:py-24">
      <div className="mx-auto max-w-3xl px-4 sm:px-6 lg:px-8">
        <div className="mb-12 mx-auto">
          <SectionHeading eyebrow="Merak Edilenler" title="Sıkça Sorulan Sorular" />
        </div>

        <div className="space-y-3">
          {faqs.map((item, index) => {
            const isOpen = openIndex === index
            return (
              <div key={item.question} className="overflow-hidden rounded-xl border border-border bg-surface">
                <button
                  type="button"
                  onClick={() => setOpenIndex(isOpen ? null : index)}
                  className="flex w-full items-center justify-between gap-4 p-5 text-left"
                  aria-expanded={isOpen}
                >
                  <span className="text-sm font-semibold text-text-primary">{item.question}</span>
                  <ChevronDownIcon
                    className={`h-5 w-5 shrink-0 text-primary transition-transform duration-200 ${
                      isOpen ? 'rotate-180' : ''
                    }`}
                  />
                </button>
                {isOpen ? (
                  <div className="border-t border-border px-5 pb-5 pt-4 text-xs leading-relaxed text-text-secondary">
                    {item.answer}
                  </div>
                ) : null}
              </div>
            )
          })}
        </div>
      </div>
    </section>
  )
}
