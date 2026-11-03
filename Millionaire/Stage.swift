import SwiftUI
import SceneKit
import UIKit

enum StageMood: Equatable {
    case intro, calm, tense, suspense, correct, wrong, milestone, victory
    var tint: UIColor {
        switch self {
        case .intro, .milestone, .victory: return .init(red: 1, green: 0.74, blue: 0.32, alpha: 1)
        case .calm: return .init(red: 0.25, green: 0.59, blue: 1, alpha: 1)
        case .tense, .suspense: return .init(red: 0.72, green: 0.36, blue: 1, alpha: 1)
        case .correct: return .init(red: 0.2, green: 0.95, blue: 0.69, alpha: 1)
        case .wrong: return .init(red: 1, green: 0.24, blue: 0.31, alpha: 1)
        }
    }
}

struct StageView: UIViewRepresentable {
    var mood: StageMood
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @Environment(\.scenePhase) private var phase

    func makeCoordinator() -> StageDirector { StageDirector() }
    func makeUIView(context: Context) -> SCNView {
        let view = SCNView()
        view.isUserInteractionEnabled = false
        view.accessibilityElementsHidden = true
        context.coordinator.install(in: view)
        return view
    }
    func updateUIView(_ view: SCNView, context: Context) {
        let economical = ProcessInfo.processInfo.isLowPowerModeEnabled || ProcessInfo.processInfo.thermalState.rawValue >= ProcessInfo.ThermalState.serious.rawValue
        view.preferredFramesPerSecond = economical ? 30 : 60
        view.antialiasingMode = economical ? .none : .multisampling2X
        context.coordinator.apply(mood, reduced: reduceMotion, economical: economical)
        view.scene?.isPaused = phase != .active || reduceMotion
        view.isPlaying = phase == .active && !reduceMotion
    }
    static func dismantleUIView(_ view: SCNView, coordinator: StageDirector) {
        view.isPlaying = false
        view.scene?.isPaused = true
        view.scene = nil
    }
}

@MainActor
final class StageDirector {
    private let world = SCNScene()
    private let accent = SCNMaterial()
    private let moon = UIColor(red: 0.35, green: 0.59, blue: 0.99, alpha: 1)
    private let gold = UIColor(red: 0.96, green: 0.73, blue: 0.35, alpha: 1)
    private var lamps: [SCNLight] = []
    private var mood: StageMood?
    private let dust = SCNParticleSystem()
    private let confetti = SCNParticleSystem()

    private func material(_ color: UIColor, shine: CGFloat = 0.4, glow: CGFloat = 0) -> SCNMaterial {
        let m = SCNMaterial()
        m.lightingModel = .blinn
        m.diffuse.contents = color
        m.specular.contents = UIColor(white: shine, alpha: 1)
        m.shininess = 0.85
        m.emission.contents = color
        m.emission.intensity = glow
        return m
    }
    @discardableResult
    private func add(_ geometry: SCNGeometry, _ position: SCNVector3, _ material: SCNMaterial, to parent: SCNNode? = nil) -> SCNNode {
        geometry.firstMaterial = material
        let n = SCNNode(geometry: geometry)
        n.position = position
        (parent ?? world.rootNode).addChildNode(n)
        return n
    }
    private func ring(_ radius: CGFloat, y: Float, thickness: CGFloat, material: SCNMaterial) {
        let shape = SCNTorus(ringRadius: radius, pipeRadius: thickness)
        shape.ringSegmentCount = 80
        shape.pipeSegmentCount = 6
        add(shape, SCNVector3(0,y,0),material)
    }
    func install(in view: SCNView) {
        world.background.contents = UIColor(red: 0.025, green: 0.035, blue: 0.095, alpha: 1)
        world.fogStartDistance = 10
        world.fogEndDistance = 29
        world.fogColor = UIColor(red: 0.03, green: 0.04, blue: 0.12, alpha: 1)
        let camera = SCNNode()
        let optics = SCNCamera()
        optics.fieldOfView = 46
        optics.zFar = 60
        optics.wantsHDR = true
        optics.bloomIntensity = 0.40
        optics.bloomThreshold = 0.93
        optics.bloomBlurRadius = 4
        optics.vignettingIntensity = 0.25
        optics.vignettingPower = 0.8
        camera.camera = optics
        camera.position = SCNVector3(3.2,3.15,7.7)
        camera.look(at: SCNVector3(0,1.02,0))
        world.rootNode.addChildNode(camera)
        view.scene = world
        view.pointOfView = camera
        let ink = material(UIColor(red: 0.075, green: 0.10, blue: 0.19, alpha: 1), shine: 0.95)
        let steel = material(UIColor(red: 0.26, green: 0.30, blue: 0.42, alpha: 1), shine: 0.92)
        let trim = material(gold, shine: 0.72, glow: 0.52)
        let blue = material(moon, glow: 0.68)
        accent.lightingModel = .constant
        accent.diffuse.contents = moon
        let ambient = SCNNode()
        ambient.light = SCNLight()
        ambient.light?.type = .ambient
        ambient.light?.color = UIColor(red: 0.56, green: 0.57, blue: 0.85, alpha: 1)
        ambient.light?.intensity = 360
        world.rootNode.addChildNode(ambient)
        for p in [SCNVector3(-3,4,4),SCNVector3(4,4,-2)] {
            let node = SCNNode()
            let lamp = SCNLight()
            lamp.type = .omni
            lamp.intensity = 950
            lamp.attenuationStartDistance = 1
            lamp.attenuationEndDistance = 12
            lamp.color = p.x < 0 ? gold : moon
            node.position = p
            node.light = lamp
            world.rootNode.addChildNode(node)
            lamps.append(lamp)
        }
        let floor = SCNFloor()
        floor.reflectivity = 0.11
        floor.reflectionFalloffEnd = 3
        floor.reflectionResolutionScaleFactor = 0.25
        add(floor, SCNVector3(0,-0.1,0),ink)
        for (radius,y,height) in [(2.55,0.0,0.14),(2.12,0.13,0.15),(1.68,0.26,0.16)] {
            let disk = SCNCylinder(radius: CGFloat(radius), height: CGFloat(height))
            disk.radialSegmentCount = 80
            add(disk,SCNVector3(0,Float(y),0),ink)
            ring(CGFloat(radius), y: Float(y+height/2), thickness: 0.018, material: radius > 2.5 ? accent : trim)
        }
        for r: CGFloat in [3.2,4.8,6.5] { ring(r, y: -0.075, thickness: 0.012, material: blue) }
        // Upholstered chair, polished stem, cantilever armrests and a foot ring.
        let chair = SCNNode()
        chair.eulerAngles.y = -0.30
        world.rootNode.addChildNode(chair)
        let upholstery = material(UIColor(red: 0.15, green: 0.13, blue: 0.31, alpha: 1), shine: 0.70)
        add(SCNCylinder(radius: 0.43,height: 0.08),SCNVector3(0,0.38,0),steel,to:chair)
        add(SCNCylinder(radius: 0.09,height: 0.63),SCNVector3(0,0.70,0),steel,to:chair)
        add(SCNTorus(ringRadius: 0.31,pipeRadius: 0.026),SCNVector3(0,0.59,0),trim,to:chair)
        add(SCNBox(width: 0.87,height: 0.17,length: 0.8,chamferRadius: 0.08),SCNVector3(0,1.02,0),upholstery,to:chair)
        let back = add(SCNBox(width: 0.82,height: 0.93,length: 0.15,chamferRadius: 0.074),SCNVector3(0,1.46,-0.34),upholstery,to:chair)
        back.eulerAngles.x = -0.14
        for side: Float in [-1,1] {
            add(SCNCylinder(radius: 0.028,height: 0.28),SCNVector3(side*0.45,1.13,0),steel,to:chair)
            add(SCNBox(width: 0.08,height: 0.065,length: 0.50,chamferRadius: 0.03),SCNVector3(side*0.45,1.27,0.02),trim,to:chair)
        }
        // LED ribs wrap around the back wall; their height follows the arc.
        let ribs = SCNBox(width: 0.11,height: 3.6,length: 0.10,chamferRadius: 0.04)
        for i in 0..<35 {
            let a = Double(i)/34*Double.pi
            let x = Float(cos(a)*7.0), z = Float(-sin(a)*6.2)
            let n = add(ribs,SCNVector3(x,1.2,z),blue)
            n.scale.y = Float(0.53+sin(a)*0.58)
        }
        for i in 0..<6 {
            let azimuth = Double(i)/6*2 * .pi
            let source = SCNVector3(Float(cos(azimuth)*4.5),4.3,Float(sin(azimuth)*4.5))
            let rig = SCNNode()
            rig.position = source
            rig.eulerAngles = SCNVector3(0,Float(-azimuth),0)
            let tilt = SCNNode()
            tilt.eulerAngles.z = -0.49
            rig.addChildNode(tilt)
            world.rootNode.addChildNode(rig)
            let cone = SCNCone(topRadius: 0.025,bottomRadius: 0.53,height: 4.5)
            cone.radialSegmentCount = 14
            let haze = material(i%2 == 0 ? moon : UIColor.systemPurple, glow: 0.7)
            haze.isDoubleSided = true
            haze.transparency = 0.045
            haze.blendMode = .add
            haze.writesToDepthBuffer = false
            add(cone,SCNVector3(0,-2.25,0),haze,to:tilt)
            let sweep = SCNAction.sequence([.rotateBy(x: 0,y: 0,z: 0.23,duration: 3.6+Double(i)*0.25),.rotateBy(x: 0,y: 0,z: -0.23,duration: 3.6+Double(i)*0.25)])
            tilt.runAction(.repeatForever(sweep))
        }
        dust.emitterShape = SCNBox(width: 7,height: 0.3,length: 7,chamferRadius: 0)
        dust.birthLocation = .volume
        dust.particleLifeSpan = 5
        dust.particleLifeSpanVariation = 1
        dust.particleSize = 0.027
        dust.particleSizeVariation = 0.013
        dust.particleVelocity = 0.34
        dust.birthDirection = .constant
        dust.emittingDirection = SCNVector3(0,1,0)
        dust.spreadingAngle = 19
        dust.particleColor = gold.withAlphaComponent(0.68)
        dust.blendMode = .additive
        world.rootNode.addParticleSystem(dust)
        let top = SCNNode()
        top.position = SCNVector3(0,4.5,0)
        world.rootNode.addChildNode(top)
        confetti.emitterShape = SCNBox(width: 6,height: 0.2,length: 4,chamferRadius: 0)
        confetti.birthLocation = .volume
        confetti.particleLifeSpan = 3.2
        confetti.particleSize = 0.072
        confetti.particleSizeVariation = 0.028
        confetti.particleColor = gold
        confetti.particleColorVariation = SCNVector4(0.16,0.20,0.06,0)
        confetti.birthDirection = .constant
        confetti.emittingDirection = SCNVector3(0,-1,0)
        confetti.particleVelocity = 1.4
        confetti.spreadingAngle = 28
        confetti.particleAngularVelocity = 120
        confetti.particleAngularVelocityVariation = 45
        confetti.birthRate = 0
        top.addParticleSystem(confetti)
    }
    func apply(_ newMood: StageMood, reduced: Bool, economical: Bool) {
        dust.birthRate = reduced ? 0 : (economical ? 3 : 8)
        confetti.birthRate = reduced ? 0 : (newMood == .victory ? 65 : (newMood == .milestone ? 25 : 0))
        guard mood != newMood else { return }
        mood = newMood
        SCNTransaction.begin()
        SCNTransaction.animationDuration = reduced ? 0 : 0.65
        accent.diffuse.contents = newMood.tint
        lamps.last?.color = newMood.tint
        lamps.last?.intensity = newMood == .suspense ? 580 : 1050
        SCNTransaction.commit()
    }
}
