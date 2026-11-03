import SwiftUI
import Combine

private let gold = Color(red: 0.96, green: 0.77, blue: 0.44)
private let mist = Color(red: 0.73, green: 0.77, blue: 0.89)
private let ink = Color(red: 0.043, green: 0.052, blue: 0.105)
private let royal = Color(red: 0.20, green: 0.17, blue: 0.40)
private let letters = ["أ", "ب", "ج", "د"]
private func arabic(_ n: Int) -> String {
    n.formatted(.number.locale(Locale(identifier: "ar_SA")))
}

private enum Page: Hashable { case intro, home, modes, game, stats, settings, rules, resume, results }
private enum Panel: String, Identifiable { case ladder, aids, call, poll; var id: String { rawValue } }

private extension Mode {
    var symbol: String {
        switch self {
        case .classic: return "crown"
        case .quick: return "bolt"
        case .daily: return "calendar"
        case .expert: return "diamond"
        case .training: return "book"
        case .endless: return "infinity"
        }
    }
    var summary: String {
        switch self {
        case .classic: return "خمسة عشر سؤالًا ومحطتا أمان"
        case .quick: return "أجب عن أكبر عدد قبل نهاية الوقت"
        case .daily: return "مجموعة أسئلة اليوم"
        case .expert: return "طريق المليون بدرجة صعوبة أعلى"
        case .training: return "تعلّم من تفسير كل إجابة"
        case .endless: return "واصل اللعب حتى أول إجابة خاطئة"
        }
    }
}
private extension Aid {
    var symbol: String {
        switch self {
        case .half: return "divide"
        case .audience: return "chart.bar.fill"
        case .friend: return "phone.fill"
        case .swap: return "arrow.triangle.2.circlepath"
        }
    }
}

private struct PressStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label.scaleEffect(configuration.isPressed ? 0.974 : 1)
            .opacity(configuration.isPressed ? 0.88 : 1)
            .animation(.easeOut(duration: 0.13), value: configuration.isPressed)
    }
}
private struct Diamond: Shape {
    func path(in r: CGRect) -> Path {
        var p = Path()
        p.move(to: CGPoint(x:r.midX,y:r.minY)); p.addLine(to: CGPoint(x:r.maxX,y:r.midY))
        p.addLine(to: CGPoint(x:r.midX,y:r.maxY)); p.addLine(to: CGPoint(x:r.minX,y:r.midY))
        p.closeSubpath(); return p
    }
}
private struct Insignia: View {
    var size: CGFloat
    var body: some View {
        ZStack {
            Circle().fill(RadialGradient(colors: [royal, ink], center: .center, startRadius: 1, endRadius: size * 0.5))
            Circle().strokeBorder(gold.opacity(0.8), lineWidth: 1)
            Circle().inset(by: 5).strokeBorder(gold.opacity(0.4), style: StrokeStyle(lineWidth: 2, dash: [1, 5]))
            ForEach(0..<3, id: \.self) { i in
                Diamond().stroke(gold.opacity(i == 0 ? 0.9 : 0.38), lineWidth: 1)
                    .frame(width:size*0.50,height:size*0.63).rotationEffect(.degrees(Double(i)*60))
            }
            Diamond().fill(gold).frame(width:size*0.13,height:size*0.21)
        }.frame(width:size,height:size).shadow(color: gold.opacity(0.18), radius: 14)
        .accessibilityHidden(true)
    }
}
private struct Primary: View {
    var label:String
    var icon:String
    var body: some View {
        HStack {
            Image(systemName:icon); Text(label).fontWeight(.bold); Spacer()
            Image(systemName:"chevron.left").font(.caption.bold())
        }.padding(.horizontal,20).frame(minHeight:54)
            .foregroundStyle(ink)
            .background(LinearGradient(colors:[Color(red:1,green:0.91,blue:0.70),gold],startPoint:.topLeading,endPoint:.bottomTrailing),in:RoundedRectangle(cornerRadius:16))
    }
}
private extension View {
    func card(_ color: Color = .white.opacity(0.12)) -> some View {
        self.padding(16)
            .background(LinearGradient(colors:[royal.opacity(0.84),ink.opacity(0.96)],startPoint:.topLeading,endPoint:.bottomTrailing),in:RoundedRectangle(cornerRadius:18))
            .overlay(RoundedRectangle(cornerRadius:18).strokeBorder(color,lineWidth:1))
    }
}
private struct MenuRow: View {
    var title:String
    var detail:String
    var icon:String
    var body: some View {
        HStack(spacing:14) {
            Image(systemName:icon).font(.title3.weight(.medium)).frame(width:40,height:40)
                .foregroundStyle(gold).background(gold.opacity(0.10),in:RoundedRectangle(cornerRadius:12))
            VStack(alignment:.leading,spacing:4) {
                Text(title).font(.headline)
                Text(detail).font(.caption).foregroundStyle(mist)
            }
            Spacer()
            Image(systemName:"chevron.left").font(.caption.bold()).foregroundStyle(mist)
        }.frame(maxWidth:.infinity,alignment:.leading).card()
    }
}
private struct Kicker: View {
    let text:String
    init(_ text:String){self.text=text}
    var body:some View {
        HStack(spacing:7) {
            Diamond().fill(gold).frame(width:7,height:7)
            Text(text).font(.caption.bold()).foregroundStyle(gold)
        }
    }
}

@MainActor
struct StudioRoot: View {
    @StateObject private var vm = GameVM()
    @StateObject private var sound = GameSound()
    @Environment(\.scenePhase) private var appPhase
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var page:Page = .intro
    @State private var panel:Panel?
    @State private var withdrawing = false
    @State private var replaceWith:Mode?
    @State private var bankError:String?
    private let clock = Timer.publish(every:0.5,on:.main,in:.common).autoconnect()

    var body:some View {
        ZStack {
            Color.black.ignoresSafeArea()
            StageView(mood:mood).ignoresSafeArea().allowsHitTesting(false).accessibilityHidden(true)
            LinearGradient(stops:[.init(color:ink.opacity(0.10),location:0),.init(color:ink.opacity(0.38),location:0.34),.init(color:ink.opacity(0.93),location:0.90)],startPoint:.top,endPoint:.bottom).ignoresSafeArea()
            if page == .intro {
                VStack(spacing:15) {
                    Insignia(size:102)
                    Text("من سيربح المليون").font(.largeTitle.bold())
                    Text("استوديو المسابقات").foregroundStyle(gold)
                }.transition(.opacity)
            } else {
                VStack(spacing:0) {
                    bar.padding(.horizontal,20).padding(.vertical,8)
                    ScrollView {
                        VStack(alignment:.leading,spacing:16) { content }
                            .frame(maxWidth:620,alignment:.leading).padding(.horizontal,20).padding(.top,12).padding(.bottom,24)
                            .frame(maxWidth:.infinity)
                    }.id(page)
                }.transition(.opacity)
            }
        }
        .foregroundStyle(.white).buttonStyle(PressStyle())
        .animation(reduceMotion ? nil : .easeInOut(duration:0.30), value:page)
        .task {
            sound.music(.intro)
            try? await Task.sleep(for:.seconds(1.3))
            if page == .intro { page = .home }
        }
        .onReceive(clock) { _ in if appPhase == .active { vm.tick() } }
        .onChange(of:appPhase) { _,newPhase in
            sound.setActive(newPhase == .active)
            if newPhase == .active { vm.tick(); syncMusic() }
        }
        .onChange(of:page) { _,_ in syncMusic() }
        .onChange(of:vm.round?.phase) { _,phase in
            guard page == .game || page == .results else { return }
            syncMusic()
            switch phase {
            case .asking: sound.cue(.question)
            case .locked: sound.cue(.lock)
            case .revealed:
                let right = vm.round?.selected == vm.question?.correct
                if right, let r = vm.round, [.classic,.expert].contains(r.mode), [4,9].contains(r.index) {
                    sound.cue(.milestone)
                } else if right { sound.cue(.correct) } else { sound.cue(.wrong) }
            case .finished: page = .results; panel = nil
                if vm.round?.payout == 1_000_000 { sound.cue(.victory); sound.cue(.applause) }
            default: break
            }
        }
        .onChange(of:vm.error) { _,message in if let message { bankError = message } }
        .sheet(item:$panel) { sheet in
            ScrollView { VStack(alignment:.leading,spacing:16) {
                HStack { Text(sheet == .ladder ? "سلم الجوائز" : sheet == .call ? "اتصال الاستوديو" : sheet == .poll ? "صوت الجمهور" : "غرفة المساعدة").font(.title2.bold()); Spacer(); Button("إغلاق"){panel=nil} }
                switch sheet {
                case .ladder: ladder
                case .aids: aids
                case .call: FriendLine(hint:vm.round?.hint ?? "") { panel=nil }
                case .poll: if let votes=vm.round?.votes {AudienceChart(votes:votes)}
                }
            }.padding(22) }
            .foregroundStyle(.white).presentationBackground(ink).presentationDragIndicator(.visible)
            .environment(\.layoutDirection,.rightToLeft).preferredColorScheme(.dark)
        }
        .alert("تأكيد الانسحاب",isPresented:$withdrawing) {
            Button("مواصلة اللعب",role:.cancel){}
            Button("انسحاب",role:.destructive){vm.withdraw()}
        } message:{Text("ستنتهي الجولة وتُسجّل نتيجتك الحالية. المبلغ المحقق " + arabic(vm.round?.earned ?? 0) + " ريال افتراضي.")}
        .alert("توجد جولة محفوظة",isPresented:Binding(get:{replaceWith != nil},set:{if !$0 {replaceWith=nil}}),presenting:replaceWith) { mode in
            Button("متابعة الجولة"){ page = .resume }
            Button("بدء جولة بديلة",role:.destructive){start(mode)}
            Button("إلغاء",role:.cancel){}
        } message:{_ in Text("يمكنك استكمال الجولة الحالية أو استبدالها بجولة جديدة.")}
        .alert("تنبيه الاستوديو",isPresented:Binding(get:{bankError != nil},set:{if !$0 {bankError=nil}})){
            Button("حسنًا"){bankError=nil}
        } message:{ Text(bankError ?? "") }
    }

    private var mood:StageMood {
        if page == .intro {return .intro}
        if page == .results && vm.round?.payout == 1_000_000 {return .victory}
        guard page == .game, let r = vm.round else {return .calm}
        if r.phase == .locked { return .suspense }
        if r.phase == .revealed {
            if r.selected != vm.question?.correct {return .wrong}
            return [4,9].contains(r.index) ? .milestone : .correct
        }
        return r.index >= 8 ? .tense : .calm
    }
    private func syncMusic() {
        switch page {
        case .intro: sound.music(.intro)
        case .game:
            if vm.round?.phase == .locked {sound.music(.suspense)}
            else if (vm.round?.index ?? 0) >= 7 {sound.music(.tense)}
            else {sound.music(.calm)}
        case .results where vm.round?.payout == 1_000_000: sound.music(.victory)
        default: sound.music(.menu)
        }
    }
    private func start(_ mode:Mode) {
        vm.start(mode)
        if let message = vm.error {bankError=message;return}
        guard let r = vm.round else {bankError="تعذّر تحميل الأسئلة.";return}
        page = r.phase == .finished ? .results : .game
        sound.cue(.question)
    }
    private var bar:some View {
        HStack {
            Insignia(size:32)
            Text("استوديو المليون").font(.subheadline.bold())
            Spacer()
            if page != .home {
                Button {page = .home} label:{
                    Label("الرئيسية",systemImage:"house").font(.caption.bold()).padding(10)
                        .background(ink.opacity(0.8),in:Capsule())
                }.accessibilityIdentifier("home")
            } else {
                Text("المستوى " + arabic(vm.level)).font(.caption.bold()).foregroundStyle(gold)
            }
        }
    }
    @ViewBuilder private var content:some View {
        switch page {
        case .home: home; homeActions
        case .modes: modes
        case .game: game
        case .results: results
        case .resume: resume
        case .rules: rules
        case .settings: settings
        case .stats: stats
        case .intro: EmptyView()
        }
    }
    private var home:some View {
        VStack(alignment:.leading,spacing:13){
            Spacer().frame(height:44)
            Kicker("المسابقة العربية")
            Text("من سيربح\nالمليون").font(.system(size:54,weight:.heavy,design:.rounded)).fixedSize(horizontal:false,vertical:true)
            Text("خمسة عشر سؤالًا تفصلك عن المليون.").font(.subheadline).foregroundStyle(mist)
        }.frame(maxWidth:.infinity,alignment:.leading)
        .padding(.bottom,6)
    }
    @ViewBuilder private var homeActions:some View {
        Button {page = .modes} label:{Primary(label:"دخول المسابقة",icon:"play.fill")}.accessibilityIdentifier("start")
        if let r = vm.round, r.phase != .finished {
            Button {page = .resume} label:{MenuRow(title:"استكمال الجولة",detail:r.mode.title + " · السؤال " + arabic(r.index+1),icon:"bookmark.fill")}.accessibilityIdentifier("resume")
        }
        Button {page = .stats} label:{MenuRow(title:"سجل المتسابق",detail:"الإنجازات وأفضل النتائج",icon:"medal")}.accessibilityIdentifier("stats")
        Button {page = .settings} label:{MenuRow(title:"غرفة التحكم",detail:"إعدادات الموسيقى والمؤثرات",icon:"slider.horizontal.3")}.accessibilityIdentifier("settings")
        Button {page = .rules} label:{MenuRow(title:"دليل المسابقة",detail:"قواعد اللعب ومحطات الأمان",icon:"text.book.closed")}.accessibilityIdentifier("rules")
        Text("جميع الجوائز افتراضية داخل اللعبة.").font(.caption).foregroundStyle(mist)
    }
    @ViewBuilder private var modes:some View {
        Kicker("قبل البث")
        Text("اختر نمط اللعب").font(.largeTitle.bold())
        ForEach([Mode.classic,.quick,.daily,.expert,.training,.endless],id:\.self){ mode in
            Button {
                if let r = vm.round,r.phase != .finished {replaceWith = mode}
                else {start(mode)}
            }label:{MenuRow(title:mode.title,detail:mode.summary,icon:mode.symbol)}
            .accessibilityIdentifier("mode." + String(describing:mode))
        }
    }
    @ViewBuilder private var resume:some View {
        if let r = vm.round {
            Kicker("جولتك المحفوظة")
            Text(r.mode.title).font(.largeTitle.bold())
            Text("السؤال " + arabic(r.index+1)).font(.title2)
            Text("الإجابات الصحيحة " + arabic(r.correctCount)).foregroundStyle(mist)
            Button {page = r.phase == .finished ? .results : .game} label:{Primary(label:"العودة إلى الاستوديو",icon:"play.fill")}.accessibilityIdentifier("continue")
        }
    }
    @Namespace private var answerSelection
    @ViewBuilder private var game:some View {
        if let r = vm.round, let q = vm.question {
            HStack(alignment:.center) {
                VStack(alignment:.leading,spacing:3) {
                    Kicker(r.mode.title)
                    Text("السؤال " + arabic(r.index+1)).font(.title2.bold())
                }
                Spacer()
                if [.classic,.expert].contains(r.mode), GameVM.prizes.indices.contains(r.index) {
                    Button {panel = .ladder} label:{
                        VStack(alignment:.trailing){Text("قيمة السؤال").font(.caption);Text(arabic(GameVM.prizes[r.index]) + " ر.س").bold().foregroundStyle(gold)}
                    }.accessibilityIdentifier("ladder")
                } else if r.mode == .quick {
                    Label(arabic(vm.secondsLeft) + " ث",systemImage:"timer").foregroundStyle(gold).monospacedDigit()
                } else {Text("صحيحة " + arabic(r.correctCount)).foregroundStyle(gold)}
            }
            if r.mode.ladder {
                HStack(spacing:4){
                    ForEach(0..<15,id:\.self){ index in
                        Capsule().fill(index < r.index ? gold : index == r.index ? Color.white : Color.white.opacity(0.17))
                            .frame(maxWidth:.infinity).frame(height:[4,9,14].contains(index) ? 6 : 3)
                    }
                }.accessibilityHidden(true)
            }
            VStack(alignment:.leading,spacing:12) {
                Text(q.category).font(.caption.bold()).foregroundStyle(gold)
                Text(q.text).font(.title3.weight(.bold)).fixedSize(horizontal:false,vertical:true)
                    .frame(maxWidth:.infinity,alignment:.leading).accessibilityIdentifier("question")
                    .id(q.id).transition(.opacity)
                if r.phase == .locked {HStack {ProgressView().tint(gold);Text("الإجابة النهائية…").font(.subheadline).foregroundStyle(gold)}}
                if r.mode.ladder && [4,9].contains(r.index) {
                    Label(r.phase == .revealed && r.selected == q.correct ? "اجتزت محطة الأمان" : "محطة أمان",systemImage:"shield.fill").font(.caption.bold()).foregroundStyle(gold)
                }
            }.frame(maxWidth:.infinity,alignment:.leading).card(gold.opacity(0.40))
            VStack(spacing:8) {
                ForEach(q.options.indices,id:\.self) { i in
                    let revealed = r.phase == .revealed
                    let right = revealed && q.correct == i
                    let wrong = revealed && r.selected == i && !right
                    let color = right ? Color.mint : wrong ? .red : r.selected == i ? gold : mist
                    let hidden = r.hidden.contains(i)
                    Button {
                        vm.choose(i); sound.cue(.select)
                    } label:{
                        HStack(spacing:12) {
                            Text(letters[i]).font(.headline).frame(width:32,height:32).foregroundStyle(color)
                                .background(color.opacity(0.12),in:Circle())
                            Text(hidden ? "إجابة محذوفة" : q.options[i]).font(.body.weight(.medium))
                                .fixedSize(horizontal:false,vertical:true)
                            Spacer()
                            if right || wrong || r.selected == i {
                                Image(systemName:right ? "checkmark.circle.fill" : wrong ? "xmark.circle.fill" : "circle.inset.filled").foregroundStyle(color)
                            }
                        }.padding(.horizontal,12).padding(.vertical,8).frame(minHeight:50)
                            .background(LinearGradient(colors:[ink.opacity(0.96),royal.opacity(0.80)],startPoint:.leading,endPoint:.trailing),in:RoundedRectangle(cornerRadius:13))
                            .overlay(RoundedRectangle(cornerRadius:13).strokeBorder(color.opacity(right || wrong || r.selected == i ? 1 : 0.28),lineWidth:1.5))
                            .overlay {
                                if r.selected == i && r.phase != .revealed {
                                    RoundedRectangle(cornerRadius:13).strokeBorder(gold,lineWidth:2)
                                        .matchedGeometryEffect(id:"chosen",in:answerSelection)
                                }
                            }.animation(reduceMotion ? nil : .spring(response:0.34,dampingFraction:0.82),value:r.selected)
                    }.accessibilityIdentifier("answer." + String(i)).disabled(r.phase != .asking || hidden).opacity(hidden ? 0.37 : 1)
                }
            }
            if r.phase == .revealed {
                VStack(alignment:.leading,spacing:7){
                    Text(r.selected == q.correct ? "إجابة صحيحة" : "الإجابة الصحيحة • " + q.options[q.correct]).font(.headline).foregroundStyle(r.selected == q.correct ? Color.mint : gold)
                    Text(q.explanation).font(.subheadline).foregroundStyle(mist)
                }.frame(maxWidth:.infinity,alignment:.leading).card()
                Button {vm.advance()}label:{Primary(label:"متابعة",icon:"arrow.left.circle.fill")}.accessibilityIdentifier("advance")
            } else if r.phase == .asking {
                if let votes = r.votes {AudienceChart(votes:votes)}
                if let hint = r.hint {Label(hint,systemImage:"phone.fill").font(.subheadline).card()}
                if r.selected != nil {
                    Button {vm.confirm()}label:{Primary(label:"تثبيت الإجابة النهائية",icon:"lock.fill")}.accessibilityIdentifier("confirm")
                }
                HStack {
                    Button {panel = .aids}label:{Label("وسائل المساعدة",systemImage:"lifepreserver").frame(minHeight:44)}.accessibilityIdentifier("aids")
                    Spacer()
                    Button("انسحاب"){withdrawing = true}.frame(minHeight:44).accessibilityIdentifier("withdraw")
                }.foregroundStyle(gold).font(.subheadline.bold())
            }
        }
    }
}
private struct AudienceChart:View{
    let votes:[Int]
    @State private var animated = false
    var body:some View {
        VStack(alignment:.leading,spacing:8){
            Label("تصويت الجمهور",systemImage:"chart.bar.fill").font(.headline)
            ForEach(votes.indices,id:\.self){i in HStack{
                Text(letters[i])
                ProgressView(value:animated ? Double(votes[i]) : 0,total:100).tint(gold)
                Text(arabic(votes[i]) + "٪").monospacedDigit()
            }}
        }.frame(maxWidth:.infinity).card().onAppear{withAnimation(.easeOut(duration:0.6)){animated=true}}
    }
}
private struct FriendLine:View{
    let hint:String
    let close:()->Void
    @State private var connected=false
    var body:some View {
        VStack(spacing:18){
            Image(systemName:connected ? "waveform.circle.fill" : "phone.circle.fill")
                .font(.system(size:64)).foregroundStyle(gold)
            Text(connected ? "سامي · صديقك الافتراضي" : "جارٍ الاتصال بسامي…").font(.headline)
            if connected {
                Text(hint).font(.title3).multilineTextAlignment(.center)
                Button(action:close){Primary(label:"إنهاء الاتصال",icon:"phone.down.fill")}
            } else { ProgressView().tint(gold) }
        }.frame(maxWidth:.infinity).card().task{
            try? await Task.sleep(for:.milliseconds(850))
            if !Task.isCancelled {withAnimation{connected=true}}
        }
    }
}
private extension StudioRoot {
    @ViewBuilder var aids:some View {
        if let r = vm.round {
            ForEach([Aid.half,.audience,.friend,.swap],id:\.self){a in
                Button {
                    sound.cue(.help); vm.use(a)
                    if a == .friend { panel = .call }
                    else if a == .audience { panel = .poll }
                    else { panel = nil }
                }label:{MenuRow(title:a.title,detail:r.used.contains(a) ? "استُخدمت في هذه الجولة" : "متاحة الآن",icon:a.symbol)}
                .disabled(r.used.contains(a) || r.phase != .asking)
                .opacity(r.used.contains(a) ? 0.43 : 1)
                .accessibilityIdentifier("aid." + String(describing:a))
            }
        }
    }
    @ViewBuilder private var ladder:some View {
        ForEach(Array(GameVM.prizes.indices.reversed()),id:\.self){i in
            HStack {
                Text(arabic(i+1)).monospacedDigit().frame(width:32)
                if [4,9].contains(i){Label("أمان",systemImage:"shield.fill").font(.caption)}
                if i == vm.round?.index {Text("السؤال الحالي").font(.caption.bold())}
                Spacer();Text(arabic(GameVM.prizes[i]) + " ريال").bold()
            }.padding(8).foregroundStyle(i == vm.round?.index || [4,9,14].contains(i) ? gold : .white)
                .background(i == vm.round?.index ? gold.opacity(0.15) : Color.clear,in:RoundedRectangle(cornerRadius:8))
        }
    }
    @ViewBuilder private var results:some View {
        if let r = vm.round {
            Insignia(size:92)
            Kicker(r.mode.title)
            Text(r.outcome ?? "انتهت الجولة").font(.largeTitle.bold())
            if [.classic,.expert].contains(r.mode) {
                Text(arabic(r.payout) + " ريال افتراضي").font(.title.bold()).foregroundStyle(gold)
            }
            VStack(alignment:.leading,spacing:9) {
                metric("الأسئلة المجابة", arabic(r.answered))
                metric("الإجابات الصحيحة", arabic(r.correctCount))
                metric("دقة الإجابة", arabic(r.answered == 0 ? 0 : 100*r.correctCount/r.answered) + "٪")
                metric("وسائل المساعدة المستخدمة", arabic(r.used.count))
            }.card()
            Button {page = .modes}label:{Primary(label:"جولة جديدة",icon:"arrow.clockwise")}.accessibilityIdentifier("newRound")
            Button {page = .stats}label:{MenuRow(title:"سجل المتسابق",detail:"عرض الإحصاءات والأوسمة",icon:"medal")}
        }
    }
    @ViewBuilder private var stats:some View {
        Kicker("بطاقة المتسابق")
        Text("الإنجازات والإحصاءات").font(.largeTitle.bold())
        VStack(alignment:.leading,spacing:11) {
            metric("مستوى اللاعب",arabic(vm.level))
            metric("الجولات المسجلة",arabic(vm.history.count))
            metric("أفضل جائزة",arabic(vm.history.map(\.payout).max() ?? 0) + " ريال")
            metric("إجابات صحيحة",arabic(vm.history.reduce(0){$0+$1.correct}))
        }.card()
        Text("الأوسمة").font(.title3.bold())
        if vm.badges.isEmpty {Text("أكمل الجولات لتحصل على أوسمتك الأولى.").foregroundStyle(mist)}
        ForEach(vm.badges,id:\.self){badge in Label(badge,systemImage:"rosette").foregroundStyle(gold)}
        Text("سجل الجولات").font(.title3.bold())
        if vm.history.isEmpty { Text("ستظهر نتائجك هنا بعد انتهاء الجولة الأولى.").foregroundStyle(mist) }
        ForEach(vm.history.sorted{$0.date > $1.date},id:\.id){entry in
            VStack(alignment:.leading,spacing:7){
                HStack{Text(entry.mode.title).bold();Spacer();Text(entry.date,style:.date).font(.caption)}
                Text(entry.outcome).foregroundStyle(gold)
                Text("صحيحة " + arabic(entry.correct) + " من " + arabic(entry.answered)).font(.caption).foregroundStyle(mist)
            }.card()
        }
    }
    private func metric(_ title:String,_ value:String)->some View {
        HStack {Text(title).foregroundStyle(mist);Spacer();Text(value).bold()}
    }
    @ViewBuilder private var settings:some View {
        Kicker("غرفة التحكم")
        Text("إعدادات الصوت").font(.largeTitle.bold())
        VStack(alignment:.leading,spacing:14){
            Toggle("موسيقى الاستوديو",isOn:$sound.musicEnabled).accessibilityIdentifier("musicToggle")
            Slider(value:$sound.musicVolume,in:0...1).accessibilityLabel("مستوى الموسيقى").disabled(!sound.musicEnabled)
            Text("مستوى الموسيقى " + arabic(Int(sound.musicVolume*100)) + "٪").font(.caption)
        }.card().tint(gold)
        VStack(alignment:.leading,spacing:14){
            Toggle("المؤثرات الصوتية",isOn:$sound.effectsEnabled).accessibilityIdentifier("effectsToggle")
            Slider(value:$sound.effectsVolume,in:0...1,onEditingChanged:{editing in if !editing{sound.cue(.select)}})
                .accessibilityLabel("مستوى المؤثرات").disabled(!sound.effectsEnabled)
            Button("تجربة صوت الإجابة الصحيحة"){sound.cue(.correct)}.disabled(!sound.effectsEnabled).foregroundStyle(gold)
        }.card().tint(gold)
    }
    @ViewBuilder private var rules:some View {
        Kicker("دليل المتسابق")
        Text("قواعد المسابقة").font(.largeTitle.bold())
        rule("طريق المليون","في النمط الكلاسيكي تجيب عن خمسة عشر سؤالًا. اختر أحد الأجوبة الأربعة ثم ثبّت اختيارك. قبل التثبيت تستطيع تغيير رأيك.")
        rule("الجائزة المضمونة","إذا أخطأت قبل إتمام السؤال الخامس تنتهي الجولة بصفر. الإجابة الصحيحة عن الخامس تضمن ألف ريال، وعن العاشر تضمن اثنين وثلاثين ألفًا. الانسحاب يمنحك آخر جائزة حققتها.")
        rule("وسائل المساعدة","احذف إجابتين، أو استطلع رأي الجمهور، أو اتصل بصديق افتراضي، أو استبدل السؤال. تُستخدم كل وسيلة مرة في الجولة. الجمهور والصديق يقدمان تقديرًا قد يصيب أو يخطئ.")
        rule("الاستكمال","يمكنك مغادرة الاستوديو والعودة إلى جولتك من الرئيسية. في التحدي السريع يستمر احتساب الوقت.")
        rule("الجوائز","الريال داخل اللعبة وحدة افتراضية لحساب النتيجة.")
    }
    private func rule(_ title:String,_ body:String)->some View {
        VStack(alignment:.leading,spacing:7){Text(title).font(.headline).foregroundStyle(gold);Text(body).font(.subheadline).lineSpacing(4)}
            .frame(maxWidth:.infinity,alignment:.leading).card()
    }
}
