// UC-STU-deactivate-student: the Deactivate action on the Students list. The search and the deactivation calls are
// stubbed, so these tests check what the page shows and sends; the backend's own tests cover the outcomes themselves.
// Run against the dev stack (backend + `npm run dev`) with `npm run cy:open`.

const john = {
  id: 4,
  username: 'j.smith@abc.edu',
  firstName: 'John',
  lastName: 'Smith',
  email: 'j.smith@abc.edu',
  enabled: true,
  teamName: 'Team1'
}
const eric = {
  id: 5,
  username: 'e.hudson@abc.edu',
  firstName: 'Eric',
  lastName: 'Hudson',
  email: 'e.hudson@abc.edu',
  enabled: false,
  teamName: 'Team1'
}

const searchResult = {
  flag: true,
  code: 200,
  message: 'Find students successfully',
  data: { content: [john, eric], number: 0, size: 50, totalElements: 2 }
}

describe('Deactivate a student', () => {
  beforeEach(() => {
    cy.login('b.gates@abc.edu', '123456')
    cy.intercept('POST', '**/students/search*', { body: searchResult }).as('search')
  })

  function openJohnsDeactivation() {
    cy.visit('/students')
    cy.wait('@search')
    cy.contains('tr', 'John').find('[data-cy=deactivate-student]').click()
    cy.get('[data-cy=deactivate-consequences]').should(
      'contain',
      'will no longer be able to log in'
    )
  }

  it('offers Deactivate only for an active student (step 3)', () => {
    cy.visit('/students')
    cy.wait('@search')
    cy.contains('tr', 'John').find('[data-cy=deactivate-student]').should('exist')
    cy.contains('tr', 'Eric').find('[data-cy=deactivate-student]').should('not.exist')
  })

  it('sends nothing when the instructor declines (5a)', () => {
    cy.intercept('PATCH', '**/students/*/deactivation', { body: {} }).as('deactivate')
    openJohnsDeactivation()

    cy.get('.deactivate-cancel').click()

    cy.get('[data-cy=deactivate-consequences]').should('not.exist')
    cy.get('@deactivate.all').should('have.length', 0)
  })

  it("deactivates on confirmation and shows the server's message (steps 5 and 6)", () => {
    cy.intercept('PATCH', '**/students/4/deactivation', {
      body: {
        flag: true,
        code: 200,
        message: 'John Smith has been deactivated.',
        data: { student: { ...john, enabled: false }, alreadyDeactivated: false, emailSent: true }
      }
    }).as('deactivate')
    openJohnsDeactivation()

    cy.get('.deactivate-confirm').click()

    cy.wait('@deactivate')
    cy.get('.el-message').should('contain', 'John Smith has been deactivated.')
  })

  it('keeps the instructor on the list when they are not permitted (6c)', () => {
    cy.intercept('PATCH', '**/students/4/deactivation', {
      statusCode: 403,
      body: { flag: false, code: 403, message: 'No permission.', data: null }
    }).as('deactivate')
    openJohnsDeactivation()

    cy.get('.deactivate-confirm').click()

    cy.wait('@deactivate')
    cy.get('.el-message').should('contain', 'You are not permitted to deactivate John Smith.')
    cy.location('pathname').should('eq', '/students')
  })
})
