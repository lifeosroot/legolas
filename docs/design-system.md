# Legolas Design System

## 1. 목적

Legolas의 화면은 Root 제품군과 같은 시각 언어를 사용한다. 이 문서는 새로운 화면을 만들 때 무엇을 기준으로 구현할지 고정한다.

디자인 시스템의 우선순위는 다음과 같다.

1. **Compose 구현 기준:** `root_android/core/designsystem`
2. **시각적 원형과 교차 검증:** `root_fe/root_fe/global/DesignSystem.swift` 및 실제 SwiftUI 화면
3. **Legolas 고유 요구:** Android 접근성, 작은 화면, 페어링 흐름

두 기준 프로젝트가 다르면 Android에서는 `root_android/core/designsystem`을 따른다. `root_fe`의 표현을 그대로 복사하기보다 같은 의미를 Compose 방식으로 구현한다.

## 2. 기준 소스

현재 로컬 기준 경로:

```text
/Users/archan/dev/personal/root/app/root_android/core/designsystem
/Users/archan/dev/personal/root/app/root_fe/root_fe/global/DesignSystem.swift
```

주요 Android 파일:

```text
core/designsystem/
├── theme/
│   ├── RootColor.kt
│   ├── RootTheme.kt
│   ├── RootType.kt
│   └── RootPreview.kt
└── component/
    ├── button/RootButton.kt
    ├── feedback/{RootLoading,RootError,RootEmpty}.kt
    ├── metric/{RootMetricGrid,RootMetricTile}.kt
    ├── section/RootSectionHeader.kt
    ├── surface/RootSurface.kt
    └── tabs/RootSegmentedTabs.kt
```

`root_android/app/.../ui/theme`의 `Purple40`, `Purple80` 기반 테마는 Android Studio 초기 템플릿이다. Legolas의 디자인 기준으로 사용하지 않는다.

## 3. 기본 원칙

- **Dark first:** 기본 배경은 항상 `#0F0F14`다.
- **고정 브랜드 색상:** 기본 accent는 `#6366F2`다.
- **Dynamic color는 기본 비활성:** 기기 배경색이 Root 브랜드 색상을 덮어쓰지 않게 한다.
- **Semantic token 사용:** 화면에서 임의의 색을 직접 만들지 않는다.
- **플랫폼 동작 존중:** 레이아웃과 상태 표현은 공유하되 Android 컴포넌트와 접근성 동작을 유지한다.
- **필요할 때만 공통 컴포넌트화:** 한 화면에서만 쓰는 UI를 미리 디자인 시스템으로 승격하지 않는다.

## 4. Color

### Core

| Token | Value | 용도 |
|---|---:|---|
| `Background` | `#0F0F14` | 앱과 화면 배경 |
| `Accent` | `#6366F2` | 주요 동작, 선택, 진행 상태 |
| `White` | `#FFFFFF` | 밝은 전경색의 원형 |
| `Black` | `#000000` | scrim의 원형 |

### Text

텍스트는 흰색에 의미별 alpha를 적용한다.

| Token | Value |
|---|---:|
| `TextPrimary` | white 92% |
| `TextSecondary` | white 60% |
| `TextTertiary` | white 50% |
| `TextDisabled` | white 35% |

### Surface and border

| Token | Value |
|---|---:|
| `SurfaceLow` | white 4% |
| `Surface` | white 6% |
| `SurfaceHigh` | white 8% |
| `SurfacePressed` | white 10% |
| `SurfaceSelected` | white 15% |
| `StrokeSubtle` | white 8% |
| `Stroke` | white 15% |
| `Divider` | white 20% |
| `Scrim` | black 58% |
| `ScrimStrong` | black 72% |

### Status

| Token | Value | 용도 |
|---|---:|---|
| `Success` | `#22C55E` | 성공, 정상 |
| `Warning` | `#F59E0B` | 주의 |
| `Danger` | `#EF4444` | 오류, 위험 |
| `Info` | `#38BDF8` | 정보 |

도메인별 팔레트는 실제 도메인이 생길 때만 추가한다. Legolas의 현재 페어링 화면에는 Core와 Status 색상만 필요하다.

## 5. Typography

Android 기본 시스템 글꼴을 사용하며 letter spacing은 `0sp`다.

| Material style | Weight | Size | Line height |
|---|---|---:|---:|
| `displaySmall` | SemiBold | 30sp | 36sp |
| `headlineMedium` | SemiBold | 24sp | 30sp |
| `titleLarge` | SemiBold | 18sp | 24sp |
| `titleMedium` | SemiBold | 15sp | 21sp |
| `titleSmall` | SemiBold | 12sp | 17sp |
| `bodyLarge` | Normal | 14sp | 20sp |
| `bodyMedium` | Normal | 12sp | 18sp |
| `bodySmall` | Normal | 11sp | 16sp |
| `labelLarge` | SemiBold | 12sp | 18sp |
| `labelMedium` | Medium | 11sp | 16sp |
| `labelSmall` | Medium | 10sp | 14sp |

화면에서 임의의 `fontSize`를 지정하기보다 가장 가까운 semantic style을 사용한다.

## 6. Shape and spacing

### Shape

| Material shape | Radius |
|---|---:|
| `extraSmall` | 4dp |
| `small` | 8dp |
| `medium` | 12dp |
| `large` | 16dp |
| `extraLarge` | 24dp |

기본 surface/card는 `12dp`, 버튼은 `10dp`, segmented tab 내부 항목은 `9dp`를 사용한다.

### Spacing

현재 기준 프로젝트에는 중앙 spacing token 객체가 없다. 실제 화면에서 반복되는 `4, 8, 10, 12, 16, 20, 24, 32dp`를 사용하되, 의미 없는 별도 상수 계층은 만들지 않는다.

- 화면 좌우 여백: 보통 `20dp` 또는 `24dp`
- 카드 내부 여백: 기본 `16dp`
- 카드 내부 항목 간격: 기본 `12dp`
- 작은 텍스트 그룹: `4dp`
- 터치 대상은 시각적 버튼 크기와 별개로 최소 `48dp`를 확보한다.

## 7. 공통 컴포넌트

Legolas에 같은 요구가 생기면 아래 Android 구현을 우선 참고한다.

| Component | 역할 | 핵심 규칙 |
|---|---|---|
| `RootSurface` | 카드/그룹 컨테이너 | Surface 6%, 12dp radius, subtle stroke, 16dp padding |
| `RootPrimaryButton` | 주요 동작 | Accent 배경, white label, 10dp radius |
| `RootSecondaryButton` | 보조 동작 | 투명 배경, stroke, accent label |
| `RootSectionHeader` | 섹션 제목 | title + 선택적 secondary subtitle |
| `RootLoading` | 로딩 피드백 | 18dp accent indicator + secondary text |
| `RootError` | 복구 가능한 오류 | danger message + 선택적 retry button |
| `RootEmpty` | 빈 상태 | surface 안의 title + 선택적 description |
| `RootSegmentedTabs` | 동일 레벨 전환 | SurfaceLow container + accent selected state |
| `RootMetricTile/Grid` | 요약 수치 | 작은 화면에서 열 수가 자동 감소 |

컴포넌트 이름은 Legolas에서도 `Root` 접두사를 유지한다. Root 제품군 전체에서 같은 계약을 뜻하기 때문이다.

## 8. Compose 적용 기준

모든 화면의 최상단에는 `RootTheme`을 한 번만 적용한다.

```kotlin
setContent {
    RootTheme {
        PairingScreen(...)
    }
}
```

화면 코드는 raw color 대신 token을 사용한다.

```kotlin
Surface(
    color = RootColors.Background,
) {
    RootSurface {
        Text(
            text = "Connect to Arwen",
            style = MaterialTheme.typography.headlineMedium,
            color = RootColors.TextPrimary,
        )
        Text(
            text = "Scan the QR code shown in the Arwen terminal.",
            style = MaterialTheme.typography.bodyLarge,
            color = RootColors.TextSecondary,
        )
        RootPrimaryButton(
            text = "Scan QR code",
            onClick = onScan,
        )
    }
}
```

Legolas가 멀티 모듈로 확장될 때 디자인 시스템의 목적지는 `core:designsystem`이다. 현재처럼 화면 수가 적은 동안에는 필요한 theme과 component만 두고, 사용하지 않는 도메인 팔레트나 컴포넌트 전체를 복사하지 않는다.

## 9. iOS와 Android 매핑

| `root_fe` SwiftUI | Legolas Compose |
|---|---|
| `DesignSystem.background` | `RootColors.Background` |
| `DesignSystem.accent` | `RootColors.Accent` |
| `Color.white.opacity(...)` | 의미에 맞는 Text/Surface/Stroke token |
| `.font(.headline)` 등 | `MaterialTheme.typography.*` |
| `RoundedRectangle(cornerRadius: 12)` | `MaterialTheme.shapes.medium` |
| SwiftUI `Button` | `RootPrimaryButton` 또는 `RootSecondaryButton` |

화면을 옮길 때 숫자와 modifier를 기계적으로 번역하지 않는다. 먼저 색상과 컴포넌트의 **의미**를 찾은 뒤 해당 Compose token/component로 매핑한다.

## 10. 구현 체크리스트

- 화면 배경이 `RootColors.Background`인가?
- `RootTheme` 밖에서 Material 컴포넌트를 사용하지 않았는가?
- raw hex와 임의 opacity 대신 semantic token을 사용했는가?
- 제목, 본문, label이 `RootTypography`에 매핑됐는가?
- primary action은 화면당 명확히 하나인가?
- loading, empty, error, disabled 상태가 필요한 경우 표현됐는가?
- 360dp 폭에서 잘리지 않는가?
- 큰 글꼴과 TalkBack에서도 동작 의미가 전달되는가?
- 공통화되지 않은 일회성 UI를 디자인 시스템에 넣지 않았는가?

## 11. 변경 규칙

1. 기존 token으로 표현할 수 있으면 새 token을 만들지 않는다.
2. 새 공통 컴포넌트는 최소 두 화면에서 동일한 시각·행동 계약이 확인된 뒤 승격한다.
3. Root 공통 색상이나 typography를 바꾸면 `root_android`와 `root_fe` 영향도도 함께 확인한다.
4. Android 구현에는 compact `360dp`와 expanded `720dp` Preview를 필요한 컴포넌트에 제공한다.
5. 이 문서와 코드가 다르면 코드를 고치거나 문서를 같은 변경에서 갱신한다.
