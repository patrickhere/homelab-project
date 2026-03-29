import SwiftUI

struct ImmichDashboard: View {
    let instanceId: UUID

    @Environment(ServicesStore.self) private var servicesStore
    @Environment(Localizer.self) private var localizer
    @Environment(\.colorScheme) private var colorScheme

    @State private var selectedInstanceId: UUID
    @State private var state: LoadableState<Void> = .idle
    @State private var serverInfo: ImmichServerInfo?
    @State private var statistics: ImmichServerStatistics?
    @State private var assetStats: ImmichAssetStatistics?

    private let serviceColor = ServiceType.immich.colors.primary

    init(instanceId: UUID) {
        self.instanceId = instanceId
        _selectedInstanceId = State(initialValue: instanceId)
    }

    var body: some View {
        ServiceDashboardLayout(
            serviceType: .immich,
            instanceId: selectedInstanceId,
            state: state,
            onRefresh: { await load(force: true) }
        ) {
            instancePicker

            if let info = serverInfo {
                heroCard(info)
            }

            if let assets = assetStats {
                assetStatsGrid(assets)
            }

            if let stats = statistics, !stats.usageByUser.isEmpty {
                usageCard(stats)
            }
        }
        .navigationTitle("Immich")
        .task(id: selectedInstanceId) {
            await load(force: true)
        }
    }

    // MARK: - Instance Picker

    private var instancePicker: some View {
        let instances = servicesStore.instances(for: .immich)
        return Group {
            if instances.count > 1 {
                VStack(alignment: .leading, spacing: 12) {
                    Text(localizer.t.dashboardInstances.sentenceCased())
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(AppTheme.textMuted)

                    ForEach(instances) { instance in
                        Button {
                            HapticManager.light()
                            selectedInstanceId = instance.id
                            servicesStore.setPreferredInstance(id: instance.id, for: .immich)
                            withAnimation(.easeInOut) {
                                serverInfo = nil
                                statistics = nil
                                assetStats = nil
                                state = .idle
                            }
                        } label: {
                            HStack(spacing: 10) {
                                Circle()
                                    .fill(instance.id == selectedInstanceId ? serviceColor : AppTheme.textMuted.opacity(0.4))
                                    .frame(width: 10, height: 10)

                                VStack(alignment: .leading, spacing: 2) {
                                    Text(instance.displayLabel)
                                        .font(.subheadline.weight(.semibold))
                                    Text(instance.url)
                                        .font(.caption)
                                        .foregroundStyle(AppTheme.textMuted)
                                        .lineLimit(1)
                                }

                                Spacer()
                            }
                            .padding(14)
                            .glassCard(tint: instance.id == selectedInstanceId ? serviceColor.opacity(0.1) : nil)
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
        }
    }

    // MARK: - Cards

    private func heroCard(_ info: ImmichServerInfo) -> some View {
        GlassCard(tint: serviceColor.opacity(colorScheme == .light ? 0.14 : 0.10)) {
            VStack(alignment: .leading, spacing: 14) {
                HStack(spacing: 12) {
                    ServiceIconView(type: .immich, size: 34)
                        .frame(width: 56, height: 56)
                        .background(serviceColor.opacity(0.13), in: RoundedRectangle(cornerRadius: 16, style: .continuous))

                    VStack(alignment: .leading, spacing: 4) {
                        Text("Immich")
                            .font(.headline.bold())
                            .lineLimit(1)
                        Text("v\(info.version)")
                            .font(.caption)
                            .foregroundStyle(AppTheme.textMuted)
                    }

                    Spacer()
                }

                if let stats = statistics {
                    HStack(spacing: 6) {
                        Image(systemName: "externaldrive.fill")
                            .font(.caption2)
                            .foregroundStyle(AppTheme.textMuted)
                        Text(Formatters.formatBytes(Double(stats.totalSize)))
                            .font(.caption)
                            .foregroundStyle(AppTheme.textSecondary)
                    }
                }
            }
            .padding(14)
        }
    }

    private func assetStatsGrid(_ assets: ImmichAssetStatistics) -> some View {
        LazyVGrid(columns: twoColumnGrid, spacing: 10) {
            GlassStatCard(
                title: "Photos",
                value: Formatters.formatNumber(assets.images),
                icon: "photo.fill",
                iconColor: serviceColor
            )
            GlassStatCard(
                title: "Videos",
                value: Formatters.formatNumber(assets.videos),
                icon: "video.fill",
                iconColor: Color(hex: "#EC4899")
            )
        }
    }

    private func usageCard(_ stats: ImmichServerStatistics) -> some View {
        GlassCard(tint: AppTheme.surface.opacity(colorScheme == .light ? 0.65 : 0.45)) {
            VStack(alignment: .leading, spacing: 12) {
                Text("Usage by User")
                    .font(.subheadline.weight(.semibold))

                ForEach(stats.usageByUser, id: \.userName) { user in
                    HStack(spacing: 10) {
                        Image(systemName: "person.circle")
                            .font(.caption)
                            .foregroundStyle(serviceColor)

                        VStack(alignment: .leading, spacing: 2) {
                            Text(user.userName)
                                .font(.caption.weight(.semibold))
                                .lineLimit(1)
                            Text("\(Formatters.formatNumber(user.photos)) photos, \(Formatters.formatNumber(user.videos)) videos")
                                .font(.caption2)
                                .foregroundStyle(AppTheme.textMuted)
                        }

                        Spacer()

                        Text(Formatters.formatBytes(Double(user.usage)))
                            .font(.caption2.weight(.semibold))
                            .foregroundStyle(AppTheme.textSecondary)
                    }
                }
            }
            .padding(14)
        }
    }

    // MARK: - Data Loading

    private func load(force: Bool) async {
        if state.isLoading { return }
        if case .loaded = state, !force { return }

        state = .loading
        do {
            guard let client = await servicesStore.immichClient(instanceId: selectedInstanceId) else {
                state = .error(.notConfigured)
                return
            }

            async let infoTask = client.getServerInfo()
            async let statsTask = client.getServerStatistics()
            async let assetTask = client.getAssetStatistics()

            let loadedInfo = try await infoTask
            let loadedStats = try await statsTask
            let loadedAssets = try await assetTask

            withAnimation(.easeInOut) {
                serverInfo = loadedInfo
                statistics = loadedStats
                assetStats = loadedAssets
                state = .loaded(())
            }
        } catch let apiError as APIError {
            state = .error(apiError)
        } catch {
            state = .error(.custom(error.localizedDescription))
        }
    }
}
