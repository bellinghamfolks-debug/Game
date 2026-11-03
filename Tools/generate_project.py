#!/usr/bin/env python3
"""Generate the checked-in, editable Xcode project using Python's standard library."""
from pathlib import Path
import argparse
import hashlib
import json
import re
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
PROJECT = ROOT / 'Millionaire.xcodeproj'
TARGETS = ('Millionaire', 'MillionaireTests', 'MillionaireUITests')
DIRECTORIES = ('Millionaire', 'Tests', 'UITests')
EXPECTED = (
    ['App.swift', 'Views.swift', 'Engine.swift', 'Stage.swift', 'Sound.swift'],
    ['EngineTests.swift'],
    ['GameUITests.swift'],
)
graph = {}


def ref(key):
    return hashlib.sha256(key.encode()).hexdigest()[:24].upper()


def put(key, isa, **attrs):
    oid = ref(key)
    assert oid not in graph
    graph[oid] = dict(isa=isa, **attrs)
    return oid


def openstep(value, depth=0):
    pad = '\t' * depth
    if isinstance(value, dict):
        items = [f'{pad}\t{openstep(k)} = {openstep(v, depth + 1)};' for k, v in value.items()]
        return '{\n' + '\n'.join(items) + '\n' + pad + '}'
    if isinstance(value, list):
        return '(' + ', '.join(openstep(v, depth + 1) for v in value) + (',' if value else '') + ')'
    value = str(value)
    if re.fullmatch(r'[a-zA-Z0-9_./]+', value):
        return value
    return json.dumps(value, ensure_ascii=False)


def build_configs(key, settings, project=False):
    configs = []
    for name in ['Debug', 'Release']:
        config = settings.copy()
        if project:
            config.update({
                'SWIFT_OPTIMIZATION_LEVEL': '-Onone' if name == 'Debug' else '-O',
                'SWIFT_ACTIVE_COMPILATION_CONDITIONS': 'DEBUG $(inherited)' if name == 'Debug' else '$(inherited)',
                'ENABLE_TESTABILITY': 'YES' if name == 'Debug' else 'NO',
                'DEBUG_INFORMATION_FORMAT': 'dwarf' if name == 'Debug' else 'dwarf-with-dsym',
            })
        configs.append(put(f'{key}-{name}', 'XCBuildConfiguration', name=name, buildSettings=config))
    return put(key + '-configs', 'XCConfigurationList', buildConfigurations=configs,
               defaultConfigurationIsVisible=0, defaultConfigurationName='Release')


def file(path, typ, key=None):
    return put('file-' + (key or path), 'PBXFileReference', lastKnownFileType=typ, path=path, sourceTree='<group>')


def project_graph():
    products, groups, targets = [], [], []
    for i, (name, directory) in enumerate(zip(TARGETS, DIRECTORIES)):
        path = ROOT / directory
        swift_files = sorted(set(EXPECTED[i]) | {f.relative_to(path).as_posix() for f in path.rglob('*.swift')})
        sources = [file(f, 'sourcecode.swift', directory+'/'+f) for f in swift_files]
        children = list(sources)
        resources = []
        if i == 0:
            children.append(file('Info.plist', 'text.plist.xml'))
            resources = [file('Resources/questions.json', 'text.json'),
                         file('Resources/Audio', 'folder'), file('Assets.xcassets', 'folder.assetcatalog')]
            resources.extend(file(pack.relative_to(path).as_posix(), 'text.json')
                             for pack in sorted((path/'Resources').glob('questionpack-*.json')))
            children.extend(resources)
        groups.append(put(directory + '-group', 'PBXGroup', path=directory, children=children, sourceTree='<group>'))
        phases = []
        for kind, files in [('Sources', sources), ('Frameworks', []), ('Resources', resources)]:
            build_files = [put(name + '-build-' + f, 'PBXBuildFile', fileRef=f) for f in files]
            phases.append(put(name + '-' + kind, 'PBX' + kind + 'BuildPhase', buildActionMask=2147483647,
                              files=build_files, runOnlyForDeploymentPostprocessing=0))
        ext = '.app' if i == 0 else '.xctest'
        product = put(name + '-product', 'PBXFileReference', explicitFileType='wrapper.application' if i == 0 else 'wrapper.cfbundle',
                      includeInIndex=0, path=name+ext, sourceTree='BUILT_PRODUCTS_DIR')
        products.append(product)
        deps = []
        settings = {
            'PRODUCT_NAME': '$(TARGET_NAME)',
            'PRODUCT_BUNDLE_IDENTIFIER': '$(BUNDLE_ID)' + ['', '.tests', '.uitests'][i],
            'LD_RUNPATH_SEARCH_PATHS': ['$(inherited)', '@executable_path/Frameworks', '@loader_path/Frameworks'],
        }
        if i == 0:
            settings.update(INFOPLIST_FILE='Millionaire/Info.plist', GENERATE_INFOPLIST_FILE='NO',
                            ASSETCATALOG_COMPILER_APPICON_NAME='AppIcon')
        else:
            settings.update(GENERATE_INFOPLIST_FILE='YES', SKIP_INSTALL='YES')
            proxy = put(name + '-proxy', 'PBXContainerItemProxy', containerPortal=ref('root'), proxyType=1,
                        remoteGlobalIDString=ref(TARGETS[0] + '-target'), remoteInfo=TARGETS[0])
            deps.append(put(name + '-dependency', 'PBXTargetDependency', target=ref(TARGETS[0] + '-target'), targetProxy=proxy))
            if i == 1:
                settings.update(BUNDLE_LOADER='$(TEST_HOST)', TEST_HOST='$(BUILT_PRODUCTS_DIR)/Millionaire.app/Millionaire')
            else:
                settings.update(TEST_TARGET_NAME=TARGETS[0])
        product_type = ['application', 'bundle.unit-test', 'bundle.ui-testing'][i]
        targets.append(put(name + '-target', 'PBXNativeTarget', name=name, productName=name,
                           buildConfigurationList=build_configs(name, settings), buildPhases=phases,
                           buildRules=[], dependencies=deps, productReference=product,
                           productType='com.apple.product-type.' + product_type))
    product_group = put('products', 'PBXGroup', name='Products', children=products, sourceTree='<group>')
    main_group = put('main-group', 'PBXGroup', children=groups+[product_group], sourceTree='<group>')
    configs = build_configs('Project', dict(
        IPHONEOS_DEPLOYMENT_TARGET='17.0', BUNDLE_ID='app.millionaire.arabic',
        SWIFT_VERSION='5.0', SDKROOT='iphoneos', SUPPORTED_PLATFORMS='iphoneos iphonesimulator',
        TARGETED_DEVICE_FAMILY='1', SUPPORTS_MACCATALYST='NO', MARKETING_VERSION='1.0',
        CURRENT_PROJECT_VERSION='1', CLANG_ENABLE_MODULES='YES', CLANG_ENABLE_OBJC_ARC='YES',
        SWIFT_EMIT_LOC_STRINGS='NO', CODE_SIGN_STYLE='Automatic',
    ), project=True)
    attrs = {oid: ({'CreatedOnToolsVersion': '16.0'} if i == 0 else
                   {'CreatedOnToolsVersion': '16.0', 'TestTargetID': targets[0]}) for i, oid in enumerate(targets)}
    root = put('root', 'PBXProject', attributes={'LastUpgradeCheck': '1600', 'TargetAttributes': attrs},
               buildConfigurationList=configs, compatibilityVersion='Xcode 14.0', developmentRegion='ar',
               hasScannedForEncodings=0, knownRegions=['ar', 'Base'], mainGroup=main_group,
               productRefGroup=product_group, projectDirPath='', projectRoot='', targets=targets)
    return {'archiveVersion': 1, 'classes': {}, 'objectVersion': 56, 'objects': graph, 'rootObject': root}


def scheme():
    doc = ET.Element('Scheme', LastUpgradeVersion='1600', version='1.7')
    def buildable(parent, i=0):
        name = TARGETS[i]
        return ET.SubElement(parent, 'BuildableReference', BuildableIdentifier='primary',
                             BlueprintIdentifier=ref(name+'-target'), BlueprintName=name,
                             BuildableName=name+('.app' if i == 0 else '.xctest'),
                             ReferencedContainer='container:Millionaire.xcodeproj')
    action = ET.SubElement(doc, 'BuildAction', parallelizeBuildables='YES', buildImplicitDependencies='YES')
    entries = ET.SubElement(action, 'BuildActionEntries')
    for i in range(3):
        other = 'YES' if i == 0 else 'NO'
        entry = ET.SubElement(entries, 'BuildActionEntry', buildForTesting='YES', buildForRunning=other,
                              buildForProfiling=other, buildForArchiving=other, buildForAnalyzing=other)
        buildable(entry, i)
    params = dict(buildConfiguration='Debug', selectedDebuggerIdentifier='Xcode.DebuggerFoundation.Debugger.LLDB',
                  selectedLauncherIdentifier='Xcode.DebuggerFoundation.Launcher.LLDB')
    action = ET.SubElement(doc, 'TestAction', **params, shouldUseLaunchSchemeArgsEnv='YES')
    tests = ET.SubElement(action, 'Testables')
    for i in [1, 2]:
        buildable(ET.SubElement(tests, 'TestableReference', skipped='NO', parallelizable='NO'), i)
    buildable(ET.SubElement(action, 'MacroExpansion'))
    launch = ET.SubElement(doc, 'LaunchAction', **params, launchStyle='0', useCustomWorkingDirectory='NO',
                           ignoresPersistentStateOnLaunch='NO', debugDocumentVersioning='YES',
                           allowLocationSimulation='YES')
    buildable(ET.SubElement(launch, 'BuildableProductRunnable', runnableDebuggingMode='0'))
    profile = ET.SubElement(doc, 'ProfileAction', buildConfiguration='Release', shouldUseLaunchSchemeArgsEnv='YES',
                            useCustomWorkingDirectory='NO', debugDocumentVersioning='YES')
    buildable(ET.SubElement(profile, 'BuildableProductRunnable', runnableDebuggingMode='0'))
    ET.SubElement(doc, 'AnalyzeAction', buildConfiguration='Debug')
    ET.SubElement(doc, 'ArchiveAction', buildConfiguration='Release', revealArchiveInOrganizer='YES')
    ET.indent(doc)
    return '<?xml version="1.0" encoding="UTF-8"?>\n' + ET.tostring(doc, encoding='unicode') + '\n'


def validate_graph(doc):
    objects = doc['objects']
    def check(value):
        if isinstance(value, dict):
            for k, v in value.items():
                check(k)
                check(v)
        elif isinstance(value, list):
            for item in value:
                check(item)
        elif isinstance(value, str) and re.fullmatch('[0-9A-F]{24}', value):
            assert value in objects, f'unresolved reference {value}'
    check(doc)
    for n in TARGETS:
        assert objects[ref(n+'-target')]['isa'] == 'PBXNativeTarget'
    assert len(set(doc['objects'][doc['rootObject']]['targets'])) == 3


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--check', action='store_true', help='Verify generated project, scheme and input paths.')
    args = parser.parse_args()
    doc = project_graph()
    validate_graph(doc)
    files = {PROJECT / 'project.pbxproj': '// !$*UTF8*$!\n' + openstep(doc) + '\n',
             PROJECT / 'xcshareddata/xcschemes/Millionaire.xcscheme': scheme()}
    for path, body in files.items():
        if args.check:
            assert path.read_text() == body, f'Project is stale: regenerate {path.name}'
        else:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(body)
    if args.check:
        for directory in DIRECTORIES:
            for oid in graph[ref(directory+'-group')]['children']:
                path = ROOT / directory / graph[oid]['path']
                assert path.exists(), f'Missing project input: {path}'
    print('Validated project graph and shared scheme' if args.check else 'Generated Millionaire.xcodeproj')


if __name__ == '__main__':
    main()
