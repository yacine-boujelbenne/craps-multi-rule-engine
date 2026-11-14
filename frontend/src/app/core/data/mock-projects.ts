import { Project } from '../models/project.model';

/**
 * Project fixtures.
 *
 * Deliberately empty: the application must not ship fabricated shipments.
 * Projects are created through the UI (or the real backend) and, in mock mode,
 * persisted to localStorage. Tests seed their own data.
 */

export const MOCK_PROJECTS: Project[] = [];
