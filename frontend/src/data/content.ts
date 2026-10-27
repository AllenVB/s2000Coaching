import type { PackageCategory } from '@/types/package'

export interface NavLink {
  label: string
  href: string
  targetId: string
  category?: PackageCategory
}

export const navLinks: NavLink[] = [
  { label: 'Ana Sayfa', href: '#ana-sayfa', targetId: 'ana-sayfa' },
  { label: 'Online Koçluk', href: '#paketler', targetId: 'paketler', category: 'KOCLUK' },
  { label: 'Beslenme', href: '#paketler-beslenme', targetId: 'paketler', category: 'BESLENME' },
  { label: 'Nasıl Çalışır?', href: '#nasil-calisir', targetId: 'nasil-calisir' },
  { label: 'Başarı Hikayeleri', href: '#basari-hikayeleri', targetId: 'basari-hikayeleri' },
  { label: 'Tecrübelerim', href: '#tecrubelerim', targetId: 'tecrubelerim' },
  { label: 'SSS', href: '#sss', targetId: 'sss' },
]

export const trustStats = [
  {
    label: 'Aktif Topluluk',
    value: '500+',
    caption: 'Aktif Öğrenci',
    footnote: 'Hedef odaklı birebir koçluk alan sporcu kitlesi.',
  },
  {
    label: 'Reçetelenen Protokol',
    value: '1.200+',
    caption: 'Hazırlanan Program',
    footnote: 'Anatomik ve metabolik ihtiyaçlara göre sıfırdan plan.',
  },
  {
    label: 'Doğrulanmış Skor',
    value: '4.9/5',
    caption: 'Ortalama Memnuniyet',
    footnote: 'Haftalık geri bildirim formları puan ortalaması.',
  },
  {
    label: 'Sürdürülebilirlik',
    value: '%95',
    caption: 'Devam Oranı',
    footnote: 'İlk 3 ayını tamamlayıp yıllık sürece geçenler.',
    accent: true,
  },
]

export const howItWorksSteps = [
  {
    step: '01',
    title: 'Paketi Seç',
    description: 'Hedefinize, bütçenize ve koçluk sürenize en uygun protokolü seçip kaydınızı güvenle başlatın.',
    meta: ['Süre: 1 Dakika', 'Anında Onay'],
  },
  {
    step: '02',
    title: 'Bilgilerini Paylaş',
    description: 'Vücut ölçülerin, spor geçmişin, sakatlık durumun ve besin tercihlerini içeren analiz formunu doldur.',
    meta: ['Süreç: Dijital Form', 'Detaylı Analiz'],
  },
  {
    step: '03',
    title: 'Programın Hazırlansın',
    description: 'Baş antrenör verilerinizi inceler. 48 saat içerisinde kişisel antrenman ve makro beslenme planınız iletilir.',
    meta: ['Teslimat: 48 Saat', 'Özel Kodlama'],
  },
  {
    step: '04',
    title: 'Takip Et & Gelİş',
    description: "Haftalık form fotoğrafları ve kilo check-in'leri yapın. Antrenman yüklerini koçunuzla sürekli optimize edin.",
    meta: ['Frekans: Haftalık', 'Sürekli Revizyon'],
  },
]

export const ecosystemFeatures = [
  {
    title: 'Kişiye özel antrenman',
    description: 'Biyomekanik yapınıza uygun hareket seçimi ve periyodizasyon.',
  },
  {
    title: 'Haftalık takip',
    description: 'Kilo, ölçüm ve form fotoğrafları üzerinden periyodik check-in.',
  },
  {
    title: 'Performans analizi',
    description: 'Ağırlık artışları, RPE değerleri ve hipertrofi hacim telemetrisi.',
  },
  {
    title: 'Program güncellemeleri',
    description: 'Plato dönemlerini aşmak için gerektiğinde dinamik revizyon.',
  },
  {
    title: 'Koç desteği',
    description: 'Sorularınız ve form video analizleriniz için doğrudan koç iletişimi.',
  },
]

export const faqs = [
  {
    question: 'Programlar gerçekten kişiye özel mi hazırlanıyor?',
    answer:
      'Evet, kesinlikle. Satın alım sonrası dolduracağınız kapsamlı analiz formundaki boy, kilo, yağ oranı, kas dengesizlikleri, sakatlık geçmişi ve spor salonunuzdaki ekipman listesi tek tek incelenir. Hazır şablon veya otomatik program asla kullanılmaz.',
  },
  {
    question: 'Koçluk paketlerinde beslenme planı dahil mi?',
    answer:
      'Evet. "Online Koçluk" paketlerimizin tamamında hem kişiye özel antrenman programı hem de gramajlı kalori ve makro beslenme planlaması birlikte eksiksiz olarak sunulur. Yalnızca beslenmesini düzenlemek isteyenler için ayrıca bağımsız beslenme paketlerimiz de vardır.',
  },
  {
    question: 'Programım ne kadar sürede teslim edilir?',
    answer:
      'Formunuzu doldurup sistemimize ilettiğiniz andan itibaren en geç 24 ila 48 saat içerisinde baş koçumuz tarafından hazırlanan protokolünüz onaylanır ve dijital kokpitinize yüklenir.',
  },
  {
    question: 'Evde antrenman yapıyorum, sisteme katılabilir miyim?',
    answer:
      'Elbette. Evinizdeki dambıl, direnç lastiği veya barfiks barı gibi mevcut ekipmanlarınıza göre özel bir ev hipertrofi & kondisyon planı tasarlayabiliriz. Spor salonuna gitme zorunluluğu yoktur.',
  },
  {
    question: 'Süreç boyunca programım revize ediliyor mu?',
    answer:
      "Kesinlikle. Gelişim hızınıza, kilo değişimine ve ağırlık platosuna göre periyodik check-in'lerde antrenman hacimleriniz ve makrolarınız düzenli olarak güncellenir.",
  },
]
