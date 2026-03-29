import SwiftUI

struct GrafanaDashboard: View {
    let instanceId: UUID

    @Environment(ServicesStore.self) private var servicesStore
    @Environment(Localizer.self) private var localizer
    @Environment(\.colorScheme) private var colorScheme

    @State private var selectedInstanceId: UUID
    @State private var state: LoadableState<Void> = .idle
    @State private var health: GrafanaHealthInfo?
    @State private var dashboards: [GrafanaDashboardSummary] = []
    @State private var alerts: [GrafanaAlert] = []

    private let serviceColor = ServiceType.grafana.colors.primary

    init(instanceId: UUID) {
        self.instanceId = instanceId
        _selectedInstanceId = State(initialValue: instanceId)
    }

    var body: some View {
        ServiceDashboardLayout(
            serviceType: .grafana,
            instanceId: selectedInstanceId,
            state: state,
            onRefresh: { await load(force: true) }
        ) {
            instancePicker

            if let health {
                heroCard(health)
            }

            statsGrid

            if !alerts.isEmpty {
                alertsCard
            }

            if !dashboards.isEmpty {
                dashboardsCard
            }
        }
        .navigationTitle("Grafana")
        .task(id: selectedInstanceId) {
            await load(force: true)
        }
    }

    // MARK: - Instance Picker

    private var instancePicker: some View {
        let instances = servicesStore.instances(for: .grafana)
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
                            servicesStore.setPreferredInstance(id: instance.id, for: .grafana)
                            withAnimation(.easeInOut) {
                                health = nil
                                dashboards = []
                                alerts = []
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

    private func heroCard(_ info: GrafanaHealthInfo) -> some View {
        GlassCard(tint: serviceColor.opacity(colorScheme == .light ? 0.14 : 0.10)) {
            VStack(alignment: .leading, spacing: 14) {
                HStack(spacing: 12) {
                    ServiceIconView(type: .grafana, size: 34)
                        .frame(width: 56, height: 56)
                        .background(serviceColor.opacity(0.13), in: RoundedRectangle(cornerRadius: 16, style: .continuous))

                    VStack(alignment: .leading, spacing: 4) {
                        Text("Grafana")
                            .font(.headline.bold())
                            .lineLimit(1)
                        Text("v\(info.version)")
                            .font(.caption)
                            .foregroundStyle(AppTheme.textMuted)
                    }

                    Spacer()
                }

                if let db = info.database {
                    HStack(spacing: 6) {
                        Image(systemName: "cylinder.fill")
                            .font(.caption2)
                            .foregroundStyle(AppTheme.textMuted)
                        Text("DB: \(db)")
                            .font(.caption)
                            .foregroundStyle(AppTheme.textSecondary)
                    }
                }
            }
            .padding(14)
        }
    }

    private var statsGrid: some View {
        LazyVGrid(columns: twoColumnGrid, spacing: 10) {
            GlassStatCard(
                title: "Dashboards",
                value: "\(dashboards.count)",
                icon: "square.grid.2x2.fill",
                iconColor: serviceColor
            )
            GlassStatCard(
                title: "Alerts",
                value: "\(alerts.count)",
                icon: "bell.badge.fill",
                iconColor: alerts.isEmpty ? AppTheme.running : Color(hex: "#EF4444")
            )
        }
    }

    private var alertsCard: some View {
        GlassCard(tint: AppTheme.surface.opacity(colorScheme == .light ? 0.65 : 0.45)) {
            VStack(alignment: .leading, spacing: 12) {
                HStack {
                    Text("Active Alerts")
                        .font(.subheadline.weight(.semibold))
                    Spacer()
                    Text("\(alerts.count)")
                        .font(.caption.bold())
                        .foregroundStyle(Color(hex: "#EF4444"))
                        .padding(.horizontal, 8)
                        .padding(.vertical, 4)
                        .background(Color(hex: "#EF4444").opacity(0.14), in: Capsule())
                }

                ForEach(alerts.prefix(10), id: \.name) { alert in
                    HStack(spacing: 10) {
                        Image(systemName: alert.state == "active" ? "exclamationmark.triangle.fill" : "bell.fill")
                            .font(.caption)
                            .foregroundStyle(alert.state == "active" ? Color(hex: "#EF4444") : AppTheme.warning)

                        VStack(alignment: .leading, spacing: 2) {
                            Text(alert.name)
                                .font(.caption.weight(.semibold))
                                .lineLimit(1)
                            HStack(spacing: 6) {
                                Text(alert.state)
                                    .font(.caption2)
                                    .foregroundStyle(AppTheme.textMuted)
                                if let severity = alert.severity {
                                    Text(severity)
                                        .font(.caption2)
                                        .foregroundStyle(AppTheme.textMuted)
                                }
                            }
                        }
                        Spacer()
                    }
                }
            }
            .padding(14)
        }
    }

    private var dashboardsCard: some View {
        GlassCard(tint: AppTheme.surface.opacity(colorScheme == .light ? 0.65 : 0.45)) {
            VStack(alignment: .leading, spacing: 10) {
                Text("Dashboards")
                    .font(.subheadline.weight(.semibold))

                ForEach(dashboards.prefix(15), id: \.uid) { dashboard in
                    HStack(spacing: 10) {
                        Image(systemName: "chart.bar.xaxis")
                            .font(.caption)
                            .foregroundStyle(serviceColor)
                            .frame(width: 24, height: 24)
                            .background(serviceColor.opacity(0.12), in: RoundedRectangle(cornerRadius: 6, style: .continuous))

                        Text(dashboard.title)
                            .font(.caption.weight(.semibold))
                            .lineLimit(1)

                        Spacer()
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
            guard let client = await servicesStore.grafanaClient(instanceId: selectedInstanceId) else {
                state = .error(.notConfigured)
                return
            }

            async let healthTask = client.getHealth()
            async let dashboardsTask = client.getDashboards()
            async let alertsTask = client.getAlerts()

            let loadedHealth = try await healthTask
            let loadedDashboards = try await dashboardsTask
            let loadedAlerts = (try? await alertsTask) ?? []

            withAnimation(.easeInOut) {
                health = loadedHealth
                dashboards = loadedDashboards
                alerts = loadedAlerts
                state = .loaded(())
            }
        } catch let apiError as APIError {
            state = .error(apiError)
        } catch {
            state = .error(.custom(error.localizedDescription))
        }
    }
}
