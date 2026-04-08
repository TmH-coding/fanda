import { get, post, put } from '@/utils/http'
export default {
  async getAll() { return get('/api/social/groups') },
  async getById(id) { return get('/api/social/groups/' + id) },
  async create(group) { return post('/api/social/groups', group) },
  async update(group) { return put('/api/social/groups/' + group.id, group) },
  async vote(groupId, candidateId) { return post('/api/social/groups/' + groupId + '/vote', { candidateId }) },
  async join(groupId) { return post('/api/social/groups/' + groupId + '/join') },
}
