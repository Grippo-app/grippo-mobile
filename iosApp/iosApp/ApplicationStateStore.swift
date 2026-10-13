import CryptoKit
import Foundation

final class ApplicationStateStore {

    private let directory = URL.applicationSupportDirectory
        .appendingPathComponent("GrippoRestoration", isDirectory: true)
    private let applicationVersion = Bundle.main.object(
        forInfoDictionaryKey: "CFBundleShortVersionString"
    ) as? String
    private let buildVersion = Bundle.main.object(
        forInfoDictionaryKey: "CFBundleVersion"
    ) as? String

    func restore() throws -> String? {
        let data: Data
        do {
            data = try Data(contentsOf: checkpointURL)
        } catch let error as CocoaError where error.code == .fileReadNoSuchFile {
            return nil
        }

        // A consumed checkpoint must not replay after decoding or launch fails.
        try clear()
        let archive = try JSONDecoder().decode(Archive.self, from: data)
        guard archive.formatVersion == Self.formatVersion,
              archive.applicationVersion == applicationVersion,
              archive.buildVersion == buildVersion,
              archive.checksum == checksum(archive.state) else {
            return nil
        }
        return archive.state
    }

    func save(_ state: String) throws {
        guard let applicationVersion, let buildVersion else {
            throw StoreError.missingApplicationMetadata
        }
        let archive = Archive(
            formatVersion: Self.formatVersion,
            applicationVersion: applicationVersion,
            buildVersion: buildVersion,
            state: state,
            checksum: checksum(state)
        )
        let data = try JSONEncoder().encode(archive)
        try FileManager.default.createDirectory(
            at: directory,
            withIntermediateDirectories: true
        )
        var protectedDirectory = directory
        var values = URLResourceValues()
        values.isExcludedFromBackup = true
        try protectedDirectory.setResourceValues(values)

        try data.write(
            to: checkpointURL,
            options: [.atomic, .completeFileProtectionUntilFirstUserAuthentication]
        )
    }

    func clear() throws {
        do {
            try FileManager.default.removeItem(at: checkpointURL)
        } catch let error as CocoaError where error.code == .fileNoSuchFile {
            return
        }
    }

    private var checkpointURL: URL {
        directory.appendingPathComponent("state.json")
    }

    private func checksum(_ state: String) -> String {
        SHA256.hash(data: Data(state.utf8))
            .map { String(format: "%02x", $0) }
            .joined()
    }

    private static let formatVersion = 1

    private struct Archive: Codable {
        let formatVersion: Int
        let applicationVersion: String
        let buildVersion: String
        let state: String
        let checksum: String
    }

    private enum StoreError: Error {
        case missingApplicationMetadata
    }
}
